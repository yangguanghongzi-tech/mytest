/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_API_PROXY_TARGET?: string
  readonly VITE_LOGIN_URL?: string
  readonly VITE_DEV_USER?: string
  readonly VITE_DEV_PASSWORD?: string
}
