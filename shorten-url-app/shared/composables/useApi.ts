import axios from 'axios'

export const useApi = () => {
  const config = useRuntimeConfig()

  const instance = axios.create({
    baseURL: config.public.apiBase
  })

  instance.interceptors.request.use((req) => {
    const token = localStorage.getItem('token')
    if (token) req.headers.Authorization = `Bearer ${token}`
    return req
  })

  instance.interceptors.response.use(
    (res) => res.data,
    (err) => {
      console.error(err)
      throw err.response?.data || err
    }
  )

  return instance
}