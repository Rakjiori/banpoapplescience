// CSRF 메타를 전역으로 노출
const csrfTokenMeta=document.querySelector('meta[name="_csrf"]');const csrfHeaderMeta=document.querySelector('meta[name="_csrf_header"]');window.csrfToken=csrfTokenMeta?csrfTokenMeta.content:'';window.csrfHeader=csrfHeaderMeta?csrfHeaderMeta.content:'X-CSRF-TOKEN';
async function registerSW(){ if(!('serviceWorker'in navigator)) return null; return navigator.serviceWorker.register('/sw.js'); }
function b64ToU8(b64){const p='='.repeat((4-b64.length%4)%4);const a=(b64+p).replace(/-/g,'+').replace(/_/g,'/');const r=atob(a),u=new Uint8Array(r.length);for(let i=0;i<r.length;i++)u[i]=r.charCodeAt(i);return u;}
async function fetchVapidPublicKey(){
  if(window.VAPID_PUBLIC) return window.VAPID_PUBLIC;
  try{
    const res = await fetch('/api/push/public-key');
    if(!res.ok) return '';
    return await res.text();
  }catch(e){ return ''; }
}

async function subscribePush(){
  if(!('Notification'in window)) { alert('브라우저가 알림을 지원하지 않습니다.'); return; }
  const perm = await Notification.requestPermission();
  if(perm!=='granted'){ alert('알림 허용이 필요합니다.'); return; }
  const reg = await registerSW();
  const pub = await fetchVapidPublicKey();
  if(!pub){ alert('VAPID 공개키가 설정되지 않았습니다.'); return; }
  const sub = await reg.pushManager.subscribe({ userVisibleOnly:true, applicationServerKey: b64ToU8(pub) });
  const json = sub.toJSON();
  await fetch('/api/push/subscribe', {
    method:'POST',
    headers:{
      'Content-Type':'application/json',
      [window.csrfHeader || 'X-CSRF-TOKEN']: window.csrfToken || ''
    },
    body: JSON.stringify({
      endpoint: json.endpoint,
      p256dh: json.keys?.p256dh,
      auth: json.keys?.auth,
      userAgent: navigator.userAgent || ''
    })
  });
  alert('브라우저 알림이 활성화되었습니다.');
}

async function unsubscribePush(){
  const reg = await registerSW();
  const sub = await reg?.pushManager.getSubscription();
  if(sub){
    const json = sub.toJSON();
    await sub.unsubscribe();
    await fetch('/api/push/unsubscribe', {
      method:'POST',
      headers:{
        'Content-Type':'application/json',
        [window.csrfHeader || 'X-CSRF-TOKEN']: window.csrfToken || ''
      },
      body: JSON.stringify({ endpoint: json.endpoint })
    });
  }
  alert('브라우저 알림이 해제되었습니다.');
}

async function scheduleNotificationsNow(){
  try{
    await fetch('/api/notifications/schedule-now', { credentials:'same-origin' });
  }catch(e){
    // ignore errors silently
  }
}

// 알림 벨 UI 상태
const notificationCenter = {
  items: [],
  seen: new Set(),
  dismissed: new Set(),
  bell: null,
  panel: null,
  list: null,
  badge: null
};

function renderNotificationCenter(){
  if(!notificationCenter.list) return;
  const { items, list, badge } = notificationCenter;
  list.innerHTML = '';
  if(!items.length){
    const empty = document.createElement('div');
    empty.className = 'notification-empty';
    empty.innerHTML = '새 알림이 없습니다.';
    list.appendChild(empty);
  } else {
    items.forEach(n=>{
      const link = document.createElement('a');
      link.className = 'notification-item';
      link.href = n.url || '/learning';
      link.addEventListener('click', ()=>{
        const key = buildNotiKey(n);
        notificationCenter.dismissed.add(key);
        persistDismissed();
        // 클릭 시 알림 제거 및 배지 갱신
        notificationCenter.items = notificationCenter.items.filter(it => it !== n);
        renderNotificationCenter();
      });
      link.innerHTML = `
        <div class="notification-title">${n.title || '알림'}</div>
        <div class="notification-body">${n.body || ''}</div>
      `;
      list.appendChild(link);
    });
  }
  if(badge){
    if(items.length){
      badge.style.display = 'inline-flex';
      badge.textContent = Math.min(items.length, 99);
    } else {
      badge.style.display = 'none';
    }
  }
}

function addNotificationsToCenter(items = []){
  if(!items || !items.length) return;
  let changed = false;
  items.forEach(n=>{
    const key = buildNotiKey(n);
    if(notificationCenter.dismissed.has(key)) return;
    if(notificationCenter.seen.has(key)) return;
    notificationCenter.seen.add(key);
    notificationCenter.items.unshift({
      title: n.title || '새 알림',
      body: n.body || '',
      url: n.url || '/learning'
    });
    changed = true;
  });
  if(notificationCenter.items.length > 20){
    notificationCenter.items = notificationCenter.items.slice(0, 20);
  }
  if(changed) renderNotificationCenter();
}

function buildNotiKey(n){
  return n.id ? `id:${n.id}` : `${n.title || ''}|${n.body || ''}|${n.url || ''}`;
}

function loadDismissed(){
  try{
    const raw = localStorage.getItem('notiDismissed');
    if(raw){
      const arr = JSON.parse(raw);
      arr.forEach(k => notificationCenter.dismissed.add(k));
    }
  }catch(e){}
}

function persistDismissed(){
  try{
    localStorage.setItem('notiDismissed', JSON.stringify(Array.from(notificationCenter.dismissed)));
  }catch(e){}
}

function setupNotificationBell(){
  notificationCenter.bell = document.getElementById('notificationBell');
  notificationCenter.panel = document.getElementById('notificationPanel');
  notificationCenter.list = document.getElementById('notificationList');
  notificationCenter.badge = document.getElementById('notificationBadge');
  if(!notificationCenter.bell || !notificationCenter.panel) return;

  notificationCenter.bell.addEventListener('click', (e)=>{
    e.preventDefault();
    const isOpen = notificationCenter.panel.classList.toggle('open');
    notificationCenter.panel.style.display = isOpen ? 'block' : 'none';
    if(!isOpen){
      // 패널을 닫을 때 클릭하지 않은 항목은 유지, 배지/렌더는 현재 상태 반영
      renderNotificationCenter();
    }
  });
  document.addEventListener('click', (e)=>{
    if(!notificationCenter.panel) return;
    if(e.target.closest('.notif-wrapper')) return;
    notificationCenter.panel.classList.remove('open');
    notificationCenter.panel.style.display = 'none';
  });
  renderNotificationCenter();
}

function setupProfileDropdown(){
  const btn = document.getElementById('profileButton');
  const panel = document.getElementById('profilePanel');
  if(!btn || !panel) return;
  btn.addEventListener('click', (e)=>{
    e.preventDefault();
    const open = panel.classList.toggle('open');
    panel.style.display = open ? 'block' : 'none';
  });
  document.addEventListener('click', (e)=>{
    if(!panel) return;
    if(e.target.closest('.profile-wrapper')) return;
    panel.classList.remove('open');
    panel.style.display = 'none';
  });
}

function setupConsultation(){
  const btns = [document.getElementById('consultBtn'), document.getElementById('scheduleConsultBtn'), document.getElementById('floatingConsultBtn')].filter(Boolean);
  const modal = document.getElementById('consultModal');
  const closeBtn = document.getElementById('consultClose');
  const form = document.getElementById('consultForm');
  if(btns.length){
    btns.forEach(btn => btn.addEventListener('click', () => {
      if(modal){ modal.style.display = 'flex'; }
    }));
  }
  closeBtn?.addEventListener('click', ()=> { if(modal) modal.style.display='none'; });
  if(modal){
    modal.addEventListener('click', (e)=>{ if(e.target === modal) modal.style.display='none'; });
  }
  if(form){
    form.addEventListener('submit', (e)=>{
      e.preventDefault();
      const data = new FormData(form);
      const type = data.get('type');
      const phone = (data.get('phone') || '').toString().trim();
      const memo = (data.get('message') || '').toString().trim();
      const message = `[연락처] ${phone}` + (memo ? ` | ${memo}` : '');
      fetch('/consultations/request', {
        method:'POST',
        headers:{
          'Content-Type':'application/json',
          [window.csrfHeader || 'X-CSRF-TOKEN']: window.csrfToken || ''
        },
        credentials:'same-origin',
        body: JSON.stringify({ type, message })
      }).then(async res=>{
        if(res.ok){
          alert('상담 요청이 접수되었습니다. (관리자에게 알림)');
          form.reset();
          if(modal) modal.style.display='none';
        } else {
          const txt = await res.text();
          alert('요청을 처리하지 못했습니다. ' + (txt || res.status));
        }
      }).catch((err)=> alert('요청을 처리하지 못했습니다. ' + err));
    });
  }
}

// 폴링 기반 웹 알림 (서버 push 없이도 동작)
async function pollNotifications(){
  try{
    const res = await fetch('/api/notifications/due', { credentials:'same-origin' });
    if(!res.ok) return;
    const items = await res.json();
    addNotificationsToCenter(items);
    if(('Notification' in window) && Notification.permission === 'granted'){
      items.forEach(n=>{
        const noti = new Notification(n.title || '새 알림', { body:n.body || '', data:{ url: n.url || '/learning' } });
        noti.onclick = () => { window.open(n.url || '/learning', '_blank'); };
      });
    }
  }catch(e){
    console.error('Notification poll error', e);
  }
}

// =======================
// Micro interactions
// =======================
function setupRevealAnimations(){
  const targets = document.querySelectorAll('[data-anim="fade-up"]');
  if(!targets.length) return;
  const io = new IntersectionObserver((entries)=>{
    entries.forEach(entry=>{
      if(entry.isIntersecting){
        const delay = entry.target.dataset.animDelay ? parseInt(entry.target.dataset.animDelay,10) : 0;
        setTimeout(()=> entry.target.classList.add('is-visible'), delay || 0);
        io.unobserve(entry.target);
      }
    });
  }, { threshold: 0.15 });
  targets.forEach(el=>io.observe(el));
}

function setupRipple(){
  document.addEventListener('click', (e)=>{
    const btn = e.target.closest('.btn, .btn-outline');
    if(!btn) return;
    const rect = btn.getBoundingClientRect();
    const ripple = document.createElement('span');
    ripple.style.position = 'absolute';
    ripple.style.borderRadius = '50%';
    ripple.style.transform = 'scale(0)';
    ripple.style.opacity = '0.3';
    ripple.style.background = 'currentColor';
    ripple.style.pointerEvents = 'none';
    ripple.style.width = ripple.style.height = Math.max(rect.width, rect.height) + 'px';
    ripple.style.left = (e.clientX - rect.left - rect.width) + 'px';
    ripple.style.top = (e.clientY - rect.top - rect.width) + 'px';
    ripple.style.transition = 'transform 0.4s ease, opacity 0.6s ease';
    ripple.className = 'btn-ripple';
    if(getComputedStyle(btn).position === 'static'){
      btn.style.position = 'relative';
    }
    btn.appendChild(ripple);
    requestAnimationFrame(()=>{
      ripple.style.transform = 'scale(1.3)';
      ripple.style.opacity = '0';
    });
    ripple.addEventListener('transitionend', ()=> ripple.remove());
  });
}

function decorateBlueButtons(){
  document.querySelectorAll('.btn:not(.btn-outline)').forEach(btn=>{
    if(!btn.classList.contains('square-spin')){
      btn.classList.add('square-spin');
    }
  });
}

document.addEventListener('DOMContentLoaded', ()=>{
  loadDismissed();
  if('Notification' in window){
    if(Notification.permission === 'default'){
      Notification.requestPermission().then(perm=>{
        if(perm === 'granted') scheduleNotificationsNow();
      });
    } else if(Notification.permission === 'granted'){
      scheduleNotificationsNow();
    }
  }
  pollNotifications();
  setInterval(pollNotifications, 60_000);
  setupNotificationBell();
  setupProfileDropdown();
  setupRevealAnimations();
  setupRipple();
  setupConsultation();
  decorateBlueButtons();
});
