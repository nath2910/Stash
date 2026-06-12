import api from './api'

const LegalProfileService = {
  async getLegalProfile() {
    const { data } = await api.get('/user/legal-profile')
    return data
  },

  async updateLegalProfile(payload) {
    const { data } = await api.put('/user/legal-profile', payload)
    return data
  },
}

export default LegalProfileService
