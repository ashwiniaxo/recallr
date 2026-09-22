import {
  createRouter,
  createWebHistory,
} from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import CourseView from '../views/CourseView.vue'
import StudyView from '../views/StudyView.vue'


const router = createRouter({
  history: createWebHistory(),

  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
    },
    {
      path: '/courses/:id',
      name: 'course',
      component: CourseView,
    },

    {
        path: '/study/sections/:sectionId',
        name: 'study',
        component: StudyView,
    },
  ],
})

export default router