import { createI18n } from 'vue-i18n'

// 英文翻译
const en = {
  nav: {
    home: 'Home',
    latestCourses: 'Latest Courses',
    famousTeachers: 'Famous Teachers',
    studyAbroad: 'Study Abroad',
    about: 'About Us',
    campus: 'Campus',
    platform: 'Teacher Recruitment',
    contact: 'Contact Us'
  },
  auth: {
    login: 'Login',
    register: 'Register',
    studentLogin: 'Student Login',
    teacherLogin: 'Teacher Login',
    studentRegister: 'Student Register',
    teacherRegister: 'Teacher Register',
    teacherCenter: 'Teacher Center',
    studentCenter: 'Student Center',
    logout: 'Logout'
  },
  footer: {
    copyright: '© 2023 GrabTeacher Personalized Teaching Platform'
  },
  lang: {
    zh: '中文',
    en: 'English'
  },
  home: {
    banner: {
      subtitle: 'Personalized Teaching',
      desc: 'Choose teachers like ordering a ride, find students like accepting orders'
    },
    sections: {
      findTeacher: {
        title: 'Find the Most Suitable Teacher for You',
        subtitle: 'Smart Matching System at Your Service',
        features: {
          quality: {
            title: 'Quality Teachers',
            desc: 'Professionally screened teachers with rich teaching experience'
          },
          matching: {
            title: 'Precise Matching',
            desc: 'Intelligently match the most suitable teacher based on learning needs and style'
          },
          personalized: {
            title: 'Personalized Teaching',
            desc: 'Tailor-made learning plans, strengthening weak areas'
          },
          flexible: {
            title: 'Flexible Time',
            desc: 'Arrange learning time freely, improve learning efficiency'
          }
        }
      },
      recommendedTeachers: {
        title: 'Recommended Teachers',
        subtitle: 'Quality teachers popular among students',
        experience: 'years of teaching',
        bookingBtn: 'Book Course'
      },
      hotCourses: {
        title: 'Popular Courses',
        subtitle: 'Selected quality course content',
        students: 'students',
        detailBtn: 'Learn More',
        viewMore: 'Browse More Courses'
      },
      testimonials: {
        title: 'Student Reviews',
        subtitle: 'Hear what they say'
      },
      cta: {
        title: 'Ready to start your learning journey?',
        subtitle: 'Register now and find the teacher that suits you best',
        button: 'Start Now'
      }
    }
  }
}

// 中文翻译
const zh = {
  nav: {
    home: '首页',
    latestCourses: '最新课程',
    famousTeachers: '天下名师',
    studyAbroad: '留学咨询',
    about: '关于我们',
    campus: '校区查询',
    platform: '教师招聘',
    contact: '联系我们'
  },
  auth: {
    login: '登录',
    register: '注册',
    studentLogin: '学生登录',
    teacherLogin: '教师登录',
    studentRegister: '学生注册',
    teacherRegister: '教师注册',
    teacherCenter: '教师中心',
    studentCenter: '学生中心',
    logout: '退出登录'
  },
  footer: {
    copyright: '© 2023 GrabTeacher 个性化教学平台'
  },
  lang: {
    zh: '中文',
    en: 'English'
  },
  home: {
    banner: {
      subtitle: '个性化教学',
      desc: '像打车一样选老师,像接单一样找学生'
    },
    sections: {
      findTeacher: {
        title: '寻找最适合您的老师',
        subtitle: '智能匹配系统为您服务',
        features: {
          quality: {
            title: '优质师资',
            desc: '经过严格筛选的专业教师，教学经验丰富'
          },
          matching: {
            title: '精准匹配',
            desc: '根据学习需求和学习风格智能匹配最适合的老师'
          },
          personalized: {
            title: '个性化教学',
            desc: '量身定制学习计划，针对薄弱环节进行强化'
          },
          flexible: {
            title: '灵活时间',
            desc: '自由安排学习时间，提高学习效率'
          }
        }
      },
      recommendedTeachers: {
        title: '推荐教师',
        subtitle: '受学生欢迎的优质教师',
        experience: '年教龄',
        bookingBtn: '预约课程'
      },
      hotCourses: {
        title: '热门课程',
        subtitle: '精选优质课程内容',
        students: '人学习',
        detailBtn: '了解详情',
        viewMore: '浏览更多课程'
      },
      testimonials: {
        title: '学员评价',
        subtitle: '听听他们怎么说'
      },
      cta: {
        title: '准备好开始您的学习之旅了吗？',
        subtitle: '立即注册并找到最适合您的老师',
        button: '立即开始'
      }
    }
  }
}

const i18n = createI18n({
  legacy: false, // 使用组合式API
  locale: 'zh', // 默认语言
  fallbackLocale: 'en', // 备用语言
  messages: {
    en,
    zh
  }
})

export default i18n
