self.addEventListener('push', e=>{
  const data = e.data?.json() || {};
  e.waitUntil(self.registration.showNotification(data.title||'새 알림', {
    body: data.body || '새 소식이 도착했어요!', data:{ url: data.url || '/' }
  }));
});
self.addEventListener('notificationclick', e=>{
  e.notification.close(); const url = e.notification?.data?.url || '/'; e.waitUntil(clients.openWindow(url));
});
