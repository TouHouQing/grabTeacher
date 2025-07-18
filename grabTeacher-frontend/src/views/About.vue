<script setup lang="ts">
// 定义组件名称
defineOptions({
  name: 'AboutView'
})
</script>

<template>
  <div class="about-page">
    <div class="banner">
      <div class="banner-content">
        <h1>关于我们</h1>
        <p>GrabTeacher - 您身边的个性化教育平台</p>
      </div>
    </div>

    <div class="container">
      <div class="section company-intro">
        <h2>公司简介</h2>
        <div class="intro-content">
          <div class="intro-image">
            <img :src="$getImageUrl('@/assets/pictures/aboutFirm.jpeg')" alt="公司图片">
          </div>
          <div class="intro-text">
            <p>GrabTeacher成立于2018年，是一家专注于个性化教育的科技公司。我们利用先进的智能匹配技术，为学生提供最适合其学习需求和学习风格的优质教师，打造真正个性化的学习体验。</p>
            <p>公司总部位于北京，目前已在上海、广州、深圳、成都等15个城市设有分支机构，拥有超过3000名经过严格筛选的专业教师，为超过50000名学生提供了优质的教育服务。</p>
            <p>我们致力于通过科技手段提升教育效率，让每一位学生都能找到最适合自己的学习方式，实现学习潜能的最大化。</p>
          </div>
        </div>
      </div>

      <div class="section mission">
        <h2>我们的使命</h2>
        <div class="mission-content">
          <div class="mission-card">
            <div class="mission-icon">
              <el-icon><Aim /></el-icon>
            </div>
            <h3>精准匹配</h3>
            <p>通过智能算法，为学生和教师之间建立精准的匹配，提高学习效率。</p>
          </div>
          <div class="mission-card">
            <div class="mission-icon">
              <el-icon><Star /></el-icon>
            </div>
            <h3>优质教育</h3>
            <p>严格筛选教师资源，确保每位学生都能享受到高质量的教育服务。</p>
          </div>
          <div class="mission-card">
            <div class="mission-icon">
              <el-icon><User /></el-icon>
            </div>
            <h3>个性化学习</h3>
            <p>根据学生的特点和需求，定制个性化的学习方案，促进全面发展。</p>
          </div>
        </div>
      </div>

      <div class="section team">
        <h2>核心团队</h2>
        <div class="team-members">
          <div class="team-member">
            <div class="member-avatar">
              <img :src="$getImageUrl('@/assets/pictures/aboutBoss1.jpeg')" alt="团队成员">
            </div>
            <h3>张明</h3>
            <p class="member-title">创始人 & CEO</p>
            <p class="member-desc">前清华大学教育学博士，拥有15年教育行业经验，曾任知名教育集团高管。</p>
          </div>
          <div class="team-member">
            <div class="member-avatar">
              <img :src="$getImageUrl('@/assets/pictures/aboutBoss2.jpeg')" alt="团队成员">
            </div>
            <h3>李华</h3>
            <p class="member-title">联合创始人 & CTO</p>
            <p class="member-desc">前Google资深工程师，人工智能领域专家，负责智能匹配系统的研发。</p>
          </div>
          <div class="team-member">
            <div class="member-avatar">
              <img :src="$getImageUrl('@/assets/pictures/aboutBoss3.jpeg')" alt="团队成员">
            </div>
            <h3>王芳</h3>
            <p class="member-title">教学总监</p>
            <p class="member-desc">北京师范大学教育学硕士，20年教学经验，负责教师培训和教学质量管理。</p>
          </div>
        </div>
      </div>

      <div class="section awards">
        <h2>荣誉与成就</h2>
        <div class="timeline">
          <div class="timeline-item">
            <div class="timeline-date">2023</div>
            <div class="timeline-content">
              <h3>教育科技创新企业50强</h3>
              <p>被评为中国教育科技创新企业50强，获得行业广泛认可。</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-date">2022</div>
            <div class="timeline-content">
              <h3>A轮融资2亿元</h3>
              <p>获得知名风投机构A轮融资2亿元，加速全国市场布局。</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-date">2021</div>
            <div class="timeline-content">
              <h3>年度最具影响力教育品牌</h3>
              <p>获得"年度最具影响力教育品牌"荣誉称号。</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-date">2020</div>
            <div class="timeline-content">
              <h3>教育部产学研合作项目</h3>
              <p>与多所重点高校合作，开展个性化教育研究项目。</p>
            </div>
          </div>
          <div class="timeline-item">
            <div class="timeline-date">2018</div>
            <div class="timeline-content">
              <h3>GrabTeacher成立</h3>
              <p>GrabTeacher在北京正式成立，开始个性化教育的探索之旅。</p>
            </div>
          </div>
        </div>
      </div>

      <div class="section contact-us">
        <h2>联系我们</h2>
        <div class="contact-info">
          <div class="contact-item">
            <el-icon><Location /></el-icon>
            <div>
              <h3>公司地址</h3>
              <p>北京市海淀区中关村科技园区8号楼</p>
            </div>
          </div>
          <div class="contact-item">
            <el-icon><Phone /></el-icon>
            <div>
              <h3>联系电话</h3>
              <p>400-888-8888</p>
            </div>
          </div>
          <div class="contact-item">
            <el-icon><Message /></el-icon>
            <div>
              <h3>电子邮箱</h3>
              <p>contact@grabteacher.com</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.about-page {
  width: 100%;
}

.banner {
  height: 300px;
  background-image: url('@/assets/pictures/aboutBackground.jpeg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  text-align: center;
  position: relative;
}

.banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
}

.banner-content {
  position: relative;
  z-index: 10;
}

.banner h1 {
  font-size: 48px;
  margin-bottom: 15px;
}

.banner p {
  font-size: 20px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
}

.section {
  margin-bottom: 80px;
}

.section h2 {
  font-size: 36px;
  text-align: center;
  margin-bottom: 40px;
  color: #333;
  position: relative;
}

.section h2::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background-color: #409EFF;
}

.intro-content {
  display: flex;
  gap: 40px;
  align-items: center;
}

.intro-image {
  flex: 1;
}

.intro-image img {
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.intro-text {
  flex: 1;
}

.intro-text p {
  margin-bottom: 20px;
  line-height: 1.8;
  color: #666;
}

.mission-content {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.mission-card {
  background-color: #fff;
  padding: 40px 30px;
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.mission-card:hover {
  transform: translateY(-10px);
}

.mission-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 20px;
}

.mission-card h3 {
  font-size: 20px;
  margin-bottom: 15px;
  color: #333;
}

.mission-card p {
  color: #666;
  line-height: 1.6;
}

.team-members {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.team-member {
  background-color: #fff;
  padding: 30px;
  text-align: center;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.member-avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 20px;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.team-member h3 {
  font-size: 20px;
  margin-bottom: 5px;
  color: #333;
}

.member-title {
  color: #409EFF;
  margin-bottom: 15px;
  font-weight: bold;
}

.member-desc {
  color: #666;
  line-height: 1.6;
}

.timeline {
  position: relative;
  max-width: 800px;
  margin: 0 auto;
}

.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 100%;
  background-color: #ddd;
}

.timeline-item {
  position: relative;
  margin-bottom: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.timeline-date {
  position: absolute;
  left: 0;
  width: 100px;
  text-align: right;
  padding-right: 30px;
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.timeline-content {
  position: relative;
  margin-left: 150px;
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  width: calc(100% - 150px);
}

.timeline-content::before {
  content: '';
  position: absolute;
  top: 20px;
  left: -40px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #409EFF;
  border: 4px solid #fff;
  box-shadow: 0 0 0 2px #409EFF;
}

.timeline-content h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.timeline-content p {
  color: #666;
  line-height: 1.6;
}

.contact-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  background-color: #fff;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.contact-item .el-icon {
  font-size: 32px;
  color: #409EFF;
}

.contact-item h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.contact-item p {
  color: #666;
}

@media (max-width: 992px) {
  .intro-content {
    flex-direction: column;
  }

  .mission-content,
  .team-members,
  .contact-info {
    grid-template-columns: repeat(2, 1fr);
  }

  .timeline-content {
    margin-left: 120px;
    width: calc(100% - 120px);
  }
}

@media (max-width: 768px) {
  .mission-content,
  .team-members,
  .contact-info {
    grid-template-columns: 1fr;
  }

  .timeline::before {
    left: 40px;
  }

  .timeline-date {
    left: 0;
    text-align: left;
    padding-left: 60px;
    padding-right: 0;
  }

  .timeline-content {
    margin-left: 80px;
    width: calc(100% - 80px);
  }

  .timeline-content::before {
    left: -50px;
  }
}
</style>
