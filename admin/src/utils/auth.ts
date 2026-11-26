const TOKEN_KEY = 'learning-admin-token'

export const getToken = () => sessionStorage.getItem(TOKEN_KEY)

export const setToken = (token: string) => sessionStorage.setItem(TOKEN_KEY, token)

export const clearToken = () => sessionStorage.removeItem(TOKEN_KEY)

export function captureTokenFromUrl() {
  const url = new URL(window.location.href)
  const token = url.searchParams.get('token')
  if (!token) return
  setToken(token)
  url.searchParams.delete('token')
  window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`)
}

export function loginUrl() {
  const configured = import.meta.env.VITE_LOGIN_URL
  if (!configured) return '/login'
  const url = new URL(configured, window.location.origin)
  url.searchParams.set('redirect_uri', window.location.href)
  return url.toString()
}

export function redirectToLogin() {
  clearToken()
  const target = loginUrl()
  if (target.startsWith('/')) window.location.replace(target)
  else window.location.assign(target)
}
