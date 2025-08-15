// 前端直传 Aliyun OSS 工具（使用浏览器 fetch）。
// 依赖：后端 /api/file/presign 接口签发 PUT 预签名 URL
// 注意：需在 OSS 桶的 CORS 中允许 PUT、OPTIONS，允许任意头（至少 Content-Type），暴露 ETag 头

export interface PresignParams {
  module: string;               // 业务目录，如 'avatar'|'video'|'doc'
  filename: string;             // 原始文件名，用于保留扩展名
  contentType: string;          // 文件 MIME 类型
  ttlSeconds?: number;          // 预签名有效期，默认 300s
}

export interface PresignResponse {
  url: string;  // 预签名 PUT URL
}

export async function getPresignedPutUrl(apiBase: string, params: PresignParams): Promise<string> {
  const query = new URLSearchParams({
    module: params.module,
    filename: params.filename,
    contentType: params.contentType,
    ttlSeconds: String(params.ttlSeconds ?? 300),
  });
  const resp = await fetch(`${apiBase}/api/file/presign?${query.toString()}`, {
    method: 'GET',
    credentials: 'include',
  });
  if (!resp.ok) throw new Error(`获取预签名URL失败: ${resp.status}`);
  const data = await resp.json();
  if (!data?.success) throw new Error(data?.message || '获取预签名URL失败');
  return data.data as string;
}

export async function uploadByPresignedUrl(presignedUrl: string, file: File): Promise<string> {
  const resp = await fetch(presignedUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type || 'application/octet-stream',
    },
    body: file,
  });
  if (!resp.ok) throw new Error(`上传失败: ${resp.status}`);
  // ETag 可用于校验，部分浏览器跨域需在OSS CORS暴露该响应头
  const etag = resp.headers.get('ETag') || undefined;
  return etag ?? 'OK';
}

// ============== 分片并行上传（Multipart Upload） ==============
// 原理：
// 1) 由后端提供多段上传初始化接口，返回 uploadId 与目标 object key
// 2) 前端按分片大小切割文件，针对每个 part 调后端获取预签名（用于 PUT 上传该 part）
// 3) 所有分片上传完，调用后端完成多段上传（带上每个 part 的 ETag）
// 4) 如需取消，调用后端中止

export interface InitMultipartResp {
  uploadId: string;
  key: string;
}

export interface PartETag {
  partNumber: number;
  eTag: string;
}

export async function initMultipart(apiBase: string, module: string, filename: string): Promise<InitMultipartResp> {
  const query = new URLSearchParams({ module, filename });
  const resp = await fetch(`${apiBase}/api/file/multipart/init?${query.toString()}`, { method: 'POST', credentials: 'include' });
  const data = await resp.json();
  if (!data?.success) throw new Error(data?.message || '初始化分片失败');
  return data.data as InitMultipartResp;
}

export async function getPartPresign(apiBase: string, key: string, uploadId: string, partNumber: number, contentType: string): Promise<string> {
  const query = new URLSearchParams({ key, uploadId, partNumber: String(partNumber), contentType });
  const resp = await fetch(`${apiBase}/api/file/multipart/presign?${query.toString()}`, { method: 'GET', credentials: 'include' });
  const data = await resp.json();
  if (!data?.success) throw new Error(data?.message || '获取分片签名失败');
  return data.data as string;
}

export async function completeMultipart(apiBase: string, key: string, uploadId: string, parts: PartETag[]): Promise<string> {
  const resp = await fetch(`${apiBase}/api/file/multipart/complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ key, uploadId, parts }),
  });
  const data = await resp.json();
  if (!data?.success) throw new Error(data?.message || '合并分片失败');
  return data.data as string; // 返回最终可访问URL
}

export async function abortMultipart(apiBase: string, key: string, uploadId: string): Promise<void> {
  const resp = await fetch(`${apiBase}/api/file/multipart/abort?key=${encodeURIComponent(key)}&uploadId=${encodeURIComponent(uploadId)}`, {
    method: 'POST',
    credentials: 'include',
  });
  const data = await resp.json();
  if (!data?.success) throw new Error(data?.message || '中止分片失败');
}

export async function multipartUpload(apiBase: string, file: File, module: string, partSize = 5 * 1024 * 1024, concurrency = 4): Promise<string> {
  const { uploadId, key } = await initMultipart(apiBase, module, file.name);
  const partCount = Math.ceil(file.size / partSize);
  const parts: PartETag[] = [];

  const queue = Array.from({ length: partCount }, (_, i) => i + 1);
  const worker = async () => {
    for (;;) {
      const partNumber = queue.shift();
      if (!partNumber) break;
      const start = (partNumber - 1) * partSize;
      const end = Math.min(start + partSize, file.size);
      const blob = file.slice(start, end);
      const url = await getPartPresign(apiBase, key, uploadId, partNumber, file.type || 'application/octet-stream');
      const resp = await fetch(url, { method: 'PUT', headers: { 'Content-Type': file.type || 'application/octet-stream' }, body: blob });
      if (!resp.ok) throw new Error(`分片 ${partNumber} 上传失败: ${resp.status}`);
      const etag = resp.headers.get('ETag');
      if (!etag) throw new Error(`分片 ${partNumber} 缺少 ETag（请在OSS CORS暴露该响应头）`);
      parts.push({ partNumber, eTag: etag.replaceAll('"', '') });
    }
  };

  const workers = Array.from({ length: concurrency }, () => worker());
  await Promise.all(workers);

  parts.sort((a, b) => a.partNumber - b.partNumber);
  try {
    return await completeMultipart(apiBase, key, uploadId, parts);
  } catch (e) {
    await abortMultipart(apiBase, key, uploadId);
    throw e;
  }
}

