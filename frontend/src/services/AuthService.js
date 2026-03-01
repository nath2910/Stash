import api from './api'

// permet d'envoyer des requetes concernant les comptes et l'authetification vers le back
class AuthService {
  async register(payload) {
    // payload = { email, password, firstName, lastName, ... } selon ton RegisterRequest
    const res = await api.post('/auth/register', payload)
    return res.data
  }

  async login(payload) {
    const res = await api.post('/auth/login', payload)

    const headerAuth = res.headers?.authorization || res.headers?.Authorization
    let token = null

    if (headerAuth && headerAuth.startsWith('Bearer ')) {
      token = headerAuth.slice(7)
    }

    token = token || res.data?.token || res.data?.accessToken

    if (!token) {
      if (import.meta.env.DEV) {
        console.error('Reponse login:', res.data, res.headers)
      }
      throw new Error('Token manquant dans la reponse /auth/login')
    }

    const user = res.data?.user ?? res.data?.utilisateur ?? null

    localStorage.setItem('snk_token', token)
    if (user) localStorage.setItem('snk_user', JSON.stringify(user))

    return { user, token }
  }

  logout() {
    localStorage.removeItem('snk_token')
    localStorage.removeItem('snk_user')
    sessionStorage.removeItem('snk_token')
    sessionStorage.removeItem('snk_user')
  }

  async changePassword(payload) {
    const res = await api.post('/auth/change-password', payload)
    return res.data
  }

  async requestPasswordReset(payload) {
    const res = await api.post('/auth/forgot-password', payload)
    return res.data
  }

  async resetPassword(payload) {
    const res = await api.post('/auth/reset-password', payload)
    return res.data
  }

  async verifyEmail(payload) {
    const res = await api.get('/auth/verify-email', { params: { token: payload.token } })
    const token = res.data?.token || res.data?.accessToken || null
    const user = res.data?.user ?? res.data?.utilisateur ?? null

    if (token) {
      localStorage.setItem('snk_token', token)
    }
    if (user) {
      localStorage.setItem('snk_user', JSON.stringify(user))
    }

    return { user, token, raw: res.data }
  }

  // Renvoie l'email de verification sans exposer l'existence du compte.
  async resendVerification(payload) {
    const res = await api.post('/auth/resend-verification', payload)
    return res.data
  }

  async me() {
    const res = await api.get('/auth/me')
    return res.data
  }

  async deleteAccount() {
    const res = await api.delete('/auth/me')
    return res.data
  }
}

export default new AuthService()
