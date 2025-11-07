async function registerSW(){ if(!('serviceWorker'in navigator)) return null; return navigator.serviceWorker.register('/sw.js'); }
function b64ToU8(b64){const p='='.repeat((4-b64.length%4)%4);const a=(b64+p).replace(/-/g,'+').replace(/_/g,'/');const r=atob(a),u=new Uint8Array(r.length);for(let i=0;i<r.length;i++)u[i]=r.charCodeAt(i);return u;}
async function subscribePush(){
  if(!('Notification'in window)) { alert('브라우저가 알림을 지원하지 않습니다.'); return; }
  const perm = await Notification.requestPermission();
  if(perm!=='granted'){ alert('알림 허용이 필요합니다.'); return; }
  const reg = await registerSW();
  const pub = window.VAPID_PUBLIC || ''; // 추후 서버에서 주입
  if(!pub){ alert('VAPID 공개키가 설정되지 않았습니다.'); return; }
  const sub = await reg.pushManager.subscribe({ userVisibleOnly:true, applicationServerKey: b64ToU8(pub) });
  console.log('Subscribed:', sub);
}
