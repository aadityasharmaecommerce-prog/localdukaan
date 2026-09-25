
'use strict';
var T="ldk-admin-1789915041-x9K2mQ7vR4";
var rl=document.getElementById('rl');if(rl)rl.href=window.location.href;
function el(tag,cls,text){var e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined&&text!==null)e.textContent=String(text);return e}
async function act(id,body){try{var r=await fetch('/v1/admin/shops/'+encodeURIComponent(id),{method:'POST',headers:{'x-admin-token':T,'content-type':'application/json'},body:JSON.stringify(body)});if(!r.ok){var d=await r.json().catch(function(){return{}});alert((d.error&&d.error.message)?d.error.message:('Error '+r.status))}}catch(e){alert(String(e))}load()}
async function load(){var out=document.getElementById('out');try{var r=await fetch('/v1/admin/shops',{headers:{'x-admin-token':T}});if(!r.ok){var d=await r.json().catch(function(){return{}});out.textContent=(d.error&&d.error.message)?d.error.message:('Error '+r.status);return}var data=await r.json();out.textContent='';var t=el('table');var head=el('tr');['Shop','Owner','City','Products','Status','Actions'].forEach(function(x){head.appendChild(el('th',null,x))});t.appendChild(head);
data.items.forEach(function(s){var tr=el('tr');
tr.appendChild(el('td',null,s.name));
tr.appendChild(el('td',null,((s.owner_name||'')+' '+(s.owner_phone||'')).trim()));
tr.appendChild(el('td',null,s.city));
tr.appendChild(el('td',null,s.product_count));
var st=el('td',s.is_published?'live':'off',(s.is_published?'LIVE':'NOT LIVE')+' / '+s.status);tr.appendChild(st);
var td=el('td');
var b1=el('button',s.is_published?'unpub':'pub',s.is_published?'Pause':'Live karo');b1.onclick=function(){act(s.id,{publish:!s.is_published})};td.appendChild(b1);
var b2=el('button',s.status==='SUSPENDED'?'act':'sus',s.status==='SUSPENDED'?'Un-suspend':'Suspend');b2.onclick=function(){act(s.id,{status:s.status==='SUSPENDED'?'ACTIVE':'SUSPENDED'})};td.appendChild(b2);
tr.appendChild(td);t.appendChild(tr)});
out.appendChild(t)}catch(e){out.textContent='Network error: '+String(e)}}
load();setInterval(load,15000);
