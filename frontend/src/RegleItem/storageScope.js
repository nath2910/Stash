const MAX_STORAGE_OWNER_LENGTH = 160

function firstNonEmptyValue(values = []) {
  for (const value of values) {
    const cleaned = String(value ?? '').trim()
    if (cleaned) return cleaned
  }
  return ''
}

export function resolveStorageOwnerId(userId) {
  if (userId && typeof userId === 'object') {
    return (
      firstNonEmptyValue([
        userId.id,
        userId.userId,
        userId.sub,
        userId.email,
        userId.username,
        userId.uid,
      ]).slice(0, MAX_STORAGE_OWNER_LENGTH) || 'guest'
    )
  }

  return firstNonEmptyValue([userId]).slice(0, MAX_STORAGE_OWNER_LENGTH) || 'guest'
}

export function scopedStorageOwner(userId) {
  const ownerId = resolveStorageOwnerId(userId)
  return ownerId === 'guest' ? 'guest' : `user:${ownerId}`
}

export function scopedStorageKey(prefix, userId) {
  return `${prefix}_${scopedStorageOwner(userId)}`
}

export function legacyScopedStorageKeys(prefix, userId) {
  return [`${prefix}_${resolveStorageOwnerId(userId)}`]
}
