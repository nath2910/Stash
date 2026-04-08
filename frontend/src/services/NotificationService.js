import api from './api'

const NotificationService = {
  list({ page = 0, size = 20, unreadFirst = true } = {}) {
    return api.get('/notifications', {
      params: { page, size, unreadFirst },
    })
  },

  unreadCount() {
    return api.get('/notifications/unread-count')
  },

  markRead(id) {
    return api.post(`/notifications/${id}/read`)
  },

  markAllRead() {
    return api.post('/notifications/read-all')
  },

  dismiss(id) {
    return api.post(`/notifications/${id}/dismiss`)
  },

  sync() {
    return api.post('/notifications/sync')
  },
}

export default NotificationService