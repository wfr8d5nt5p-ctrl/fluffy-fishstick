const TOKEN_KEY = 'admin_token'
const STORE_KEY = 'admin_store_id'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoreId() {
  return localStorage.getItem(STORE_KEY)
}

export function setStoreId(id) {
  localStorage.setItem(STORE_KEY, id)
}

export function removeStoreId() {
  localStorage.removeItem(STORE_KEY)
}