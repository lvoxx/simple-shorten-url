import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

export class ApiService {
  private client: AxiosInstance

  constructor(baseURL?: string) {
    this.client = axios.create({
      baseURL: baseURL ?? import.meta.env?.VITE_API_BASE ?? 'http://localhost:8080',
    })

    this.client.interceptors.request.use((config) => {
      const token = this.getToken()
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    })

    this.client.interceptors.response.use(
      (response) => response.data,
      (error) => {
        const problem = error.response?.data ?? error
        return Promise.reject(problem)
      }
    )
  }

  protected getToken(): string | null {
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem('token')
    }
    return null
  }

  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.client.get(url, config) as Promise<T>
  }

  async post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return this.client.post(url, data, config) as Promise<T>
  }

  async put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return this.client.put(url, data, config) as Promise<T>
  }

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.client.delete(url, config) as Promise<T>
  }
}
