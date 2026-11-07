import test from 'node:test';
import assert from 'node:assert/strict';
import {emptySnapshot} from '../src/domain/model.mjs';
import {memoryRepository,openRepository,ConflictError} from '../src/data/repository.mjs';

test('demo repository copies data and rejects stale writers',async()=>{
  const initial=emptySnapshot(),repo=memoryRepository(initial);
  initial.applied.scale=120;assert.equal((await repo.load()).data.applied.scale,100);
  const a=await repo.load(),b=await repo.load();a.data.events.push({id:1,date:'2026-11-07',title:'Saved',time:'',completed:true});
  const saved=await repo.save(a.data,a.revision);assert.equal(saved.revision,1);
  saved.data.events[0].title='Changed outside repository';
  await assert.rejects(repo.save(b.data,b.revision),ConflictError);
  const current=await repo.load();assert.equal(current.data.events[0].title,'Saved');assert.equal(current.data.events[0].completed,true);
});
test('invalid snapshot never replaces saved data',async()=>{
  const repo=memoryRepository(),initial=await repo.load();initial.data.events.push({id:1,date:'2026-11-07',title:'Keep',time:'',completed:false});
  const saved=await repo.save(initial.data,0);saved.data.events[0].title='';
  await assert.rejects(repo.save(saved.data,1),{message:'title_required'});
  const loaded=await repo.load();assert.equal(loaded.revision,1);assert.equal(loaded.data.events[0].title,'Keep');
});
test('unavailable browser storage fails explicitly',async()=>{
  await assert.rejects(openRepository({indexedDB:null}),{message:'storage_unavailable'});
});
