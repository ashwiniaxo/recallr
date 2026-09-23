import {
  createRouter,
  createWebHistory,
} from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import CourseView from '../views/CourseView.vue'
import StudyView from '../views/StudyView.vue'
import MaterialsView from '../views/MaterialsView.vue'


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
      path: '/courses/:courseId/sections/:sectionId/study',
      name: 'study',
      component: StudyView,
    },

    {
        path: '/courses/:courseId/sections/:sectionId/materials',
        name: 'materials',
       component: MaterialsView,
    },
  ],
})

export default router