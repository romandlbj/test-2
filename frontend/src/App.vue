<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Activity, Boxes, Building2, CalendarDays, ChevronDown, ChevronLeft, ChevronRight,
  ClipboardList, Download, FileBox, Filter, Home, LayoutDashboard, Menu, Package,
  Pill, RefreshCw, Search, Settings, SlidersHorizontal, Trash2, TrendingDown,
  Truck, Warehouse, X, CheckCircle2, AlertTriangle, Eye
} from 'lucide-vue-next'

const modules = [
  { label: '首页', icon: LayoutDashboard },
  { label: '盘点库存', icon: ClipboardList, active: true },
  { label: '要货指导', icon: Boxes },
  { label: '医院供货', icon: Building2 },
  { label: '终端销量', icon: TrendingDown },
  { label: '整袋库存', icon: Package },
  { label: '零散库存', icon: FileBox },
  { label: '医院库存', icon: Warehouse },
  { label: '期初库存', icon: CalendarDays },
  { label: '中药颗粒', icon: Pill },
  { label: '医院管理', icon: Building2 }
]
const currentModule = ref('stocktake')
const genericModule = ref('')
const genericRows = ref([])
const genericSummary = ref({})
const genericTotal = ref(0)
const hospitalRows = ref([])
const goodsRows = ref([])
const goodsLoaded = ref(false)

const systemModules = [
  { label: '系统日志', icon: ClipboardList },
  { label: '系统管理', icon: Settings },
  { label: '系统监控', icon: Activity }
]

const filters = reactive({ hospitalId: '', materialCode: '', drugName: '', startDate: '', endDate: '' })
const hospitals = ref([])
const hospitalKeyword = ref('')
const rows = ref([])
const hasLoadedRows = ref(false)
const selectedIds = ref([])
const loading = ref(false)
const apiOnline = ref(false)
const notice = ref({ type: '', text: '' })
const modal = reactive({ visible: false, kind: '', title: '', message: '', reason: '来源数据修正，重新汇总', busy: false, count: 0 })
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const sort = reactive({ field: 'summaryDate', order: 'desc' })
const summaryFromApi = ref(null)
const goodsSummaryFromApi = ref(null)
const detailModal = reactive({ visible: false, loading: false, data: null, error: '' })

const genericMeta = {
  'hospital-supply': ['医院供货','供货日期','供货量'], 'terminal-sales': ['终端销量','销量日期','销量颗粒量'],
  'whole-inventory': ['整袋库存','盘点日期','整袋颗粒量'], 'loose-inventory': ['零散库存','盘点日期','零散颗粒量'],
  'hospital-inventory': ['医院库存','盘点日期','医院颗粒量'], 'opening-inventory': ['期初库存','期初日期','期初颗粒量'],
  'herbal-granules': ['中药颗粒','生效日期','颗粒单价']
}

const fallbackRows = [
  row(1, '河北北方学院附属第一医院', '02.220001', '茯苓皮', 2, 200, 268.57, 468.57, 0, 14088, .21, -136, 2.56, -348.16, .54, 1.29, .02, 242.16, 0),
  row(2, '河北北方学院附属第一医院', '02.220002', '香加皮', 1, 100, 244.2, 344.2, 0, 1229, .05, -9, 2.37, -21.33, .12, 1.2, .02, 191.22, 0),
  row(3, '河北北方学院附属第一医院', '02.220003', '醋五灵脂', 2, 200, 166.67, 366.67, 0, 1936, .2, -15, 1.8, -27, .36, 1.61, .02, 151.83, 0),
  row(4, '河北北方学院附属第一医院', '02.220004', '益智仁', 0, 0, 166.33, 166.33, 0, 105, .04, 0, 1.6, 0, .06, 1.05, .02, 105.61, 0),
  row(5, '河北北方学院附属第一医院', '02.220005', '茵草', 0, 0, 263.9, 263.9, 0, 15, .27, 2, 2.13, 4.26, .58, 2.14, .03, 82.21, 0),
  row(6, '河北北方学院附属第一医院', '02.220006', '浸骨碎补', 7, 700, 194.93, 894.93, 0, 5655, .97, -47, 1.58, -74.26, 1.53, 7.79, .12, 76.59, 0),
  row(7, '河北北方学院附属第一医院', '02.220007', '虎杖', 2, 200, 153.26, 353.26, 0, 1570, .39, -12, .57, -6.84, .22, 3.13, .05, 75.24, 0),
  row(8, '河北北方学院附属第一医院', '02.220008', '桑螵蛸', 0, 0, 101.2, 101.2, 0, 1058, .11, -9, 31.1, -279.9, 3.42, .9, .01, 74.96, 0),
  row(9, '河北北方学院附属第一医院', '02.220009', '酒黄芩', 13, 1300, 233.75, 1533.75, 0, 3529, 2.51, -20, .54, -10.8, 1.36, 15.08, .23, 67.81, 0),
  row(10, '河北北方学院附属第一医院', '02.220010', '苏木', 1, 100, 184.5, 284.5, 0, 2624, .65, -23, 1.32, -30.36, .86, 3.11, .05, 60.99, 0)
]

function row (id, hospitalName, materialCode, drugName, bagCount, wholeParticleQty, looseParticleQty, actualStockParticles, hospitalShortageParticles, hospitalParticleQty, hospitalIncreaseBags, hospitalLossBags, particleUnitPrice, hospitalLossAmount, factoryIncreaseAmount, monthlyAvgConsumption, currentMonthDemandBags, availableMonths, thirtyDayPurchaseBags) {
  return {
    id, summaryDate: '2025-12-26', hospitalId: 1, hospitalName, materialCode, drugName, bagCount,
    wholeParticleQty, looseParticleQty, actualStockParticles, hospitalShortageParticles, hospitalParticleQty,
    factoryIncreaseBags: hospitalIncreaseBags, hospitalLossBags, particleUnitPrice, hospitalLossAmount,
    factoryIncreaseAmount, guidanceStartDate: '2025-08-20', guidanceEndDate: '2025-12-26', monthlyAvgConsumption,
    currentMonthDemandBags, availableMonths, thirtyDayPurchaseBags, status: 'CONFIRMED'
  }
}

const visibleRows = computed(() => hasLoadedRows.value ? rows.value : fallbackRows)
const selectableRows = computed(() => currentModule.value === 'goods' ? goodsRows.value : visibleRows.value)
const allSelected = computed(() => selectableRows.value.length > 0 && selectableRows.value.every(item => selectedIds.value.includes(item.id)))
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const summary = computed(() => summaryFromApi.value || visibleRows.value.reduce((acc, item) => {
  ;['bagCount', 'wholeParticleQty', 'looseParticleQty', 'actualStockParticles', 'hospitalShortageParticles', 'hospitalParticleQty', 'factoryIncreaseBags', 'hospitalLossBags', 'hospitalLossAmount', 'factoryIncreaseAmount', 'monthlyAvgConsumption', 'currentMonthDemandBags', 'thirtyDayPurchaseBags'].forEach(key => { acc[key] = (acc[key] || 0) + Number(item[key] || 0) })
  return acc
}, {}))
const goodsSummary = computed(() => goodsSummaryFromApi.value || goodsRows.value.reduce((acc, item) => {
  ;['openingStockParticles','hospitalSupplyParticles','terminalSalesParticles','theoreticalStockParticles','hospitalTabletQty','hospitalParticleQty','hospitalGainLossQty','hospitalGainLossAmount','monthlyAvgConsumption','currentMonthDemandBags','thirtyDayPurchaseBags'].forEach(k => { acc[k] = (acc[k] || 0) + Number(item[k] || 0) })
  return acc
}, {}))

const currentRows = computed(() => currentModule.value === 'goods' ? goodsRows.value : visibleRows.value)

function display (value, digits = 3) {
  if (value === null || value === undefined || value === '') return '—'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: digits })
}
function displayAmount (value) {
  if (value === null || value === undefined || value === '') return '—'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function displayPrice (value) {
  if (value === null || value === undefined || value === '') return '—'
  return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 6 })
}
function statusLabel (item) {
  if (item.calculationStatus === 'WARNING' || item.warnings) return '有告警'
  if (item.calculationStatus === 'FAILED') return '计算失败'
  return item.status === 'VOID' ? '已作废' : '已汇总'
}
function warningText (item) {
  if (!item) return ''
  if (Array.isArray(item.warningCodes) && item.warningCodes.length) return item.warningCodes.join('、')
  return item.warnings || ''
}
function notify (text, type = 'success') {
  notice.value = { text, type }
  window.clearTimeout(notify.timer)
  notify.timer = window.setTimeout(() => { notice.value = { type: '', text: '' } }, 3200)
}
function queryParams () {
  const params = new URLSearchParams({ page: page.value, pageSize: pageSize.value, sortField: sort.field, sortOrder: sort.order })
  if (filters.hospitalId) params.set('hospitalId', filters.hospitalId)
  if (filters.materialCode.trim()) params.set('materialCode', filters.materialCode.trim())
  if (filters.drugName.trim()) params.set('drugName', filters.drugName.trim())
  if (filters.startDate) params.set('startDate', filters.startDate)
  if (filters.endDate) params.set('endDate', filters.endDate)
  return params
}
function queryPayload () {
  return {
    hospitalId: filters.hospitalId || null,
    materialCode: filters.materialCode.trim() || null,
    drugName: filters.drugName.trim() || null,
    startDate: filters.startDate || null,
    endDate: filters.endDate || null,
    page: 1,
    pageSize: 200,
    sortField: sort.field,
    sortOrder: sort.order
  }
}
async function loadHospitals (keyword = '') {
  try {
    const params = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
    const response = await fetch(`/api/terminal/stocktake/hospitals${params}`)
    if (!response.ok) { const error = new Error(response.status === 401 ? '未登录，无法加载医院列表' : response.status === 403 ? '没有医院数据权限' : '医院列表加载失败'); error.status = response.status; throw error }
    hospitals.value = await response.json()
    apiOnline.value = true
  } catch (error) {
    if (error.status === 401 || error.status === 403) { hospitals.value = []; apiOnline.value = false; notify(error.message, 'error'); return }
    hospitals.value = [{ id: 1, hospitalName: '河北北方学院附属第一医院' }, { id: 2, hospitalName: '石家庄市中医院' }]
  }
}
function searchHospitals () {
  filters.hospitalId = ''
  window.clearTimeout(searchHospitals.timer)
  searchHospitals.timer = window.setTimeout(() => loadHospitals(hospitalKeyword.value.trim()), 240)
}
function chooseHospital () {
  const selected = hospitals.value.find(item => item.hospitalName === hospitalKeyword.value)
  filters.hospitalId = selected ? selected.id : ''
}
async function search (resetPage = true) {
  if (!validateDateRange()) return
  if (genericModule.value) return loadGenericModule(genericModule.value)
  if (currentModule.value === 'hospitals') return loadHospitalsModule()
  if (currentModule.value === 'goods') return loadGoodsGuidance(resetPage)
  if (resetPage) page.value = 1
  loading.value = true
  try {
    const response = await fetch(`/api/terminal/stocktake?${queryParams()}`)
    if (!response.ok) { const error = new Error(response.status === 401 ? '未登录，无法查询盘点库存' : response.status === 403 ? '没有盘点库存数据权限' : '盘点库存查询失败'); error.status = response.status; throw error }
    const data = await response.json()
    rows.value = data.items || []
    hasLoadedRows.value = true
    total.value = data.total || rows.value.length
    summaryFromApi.value = data.summary || null
    apiOnline.value = true
    notify(`查询完成，共 ${total.value} 条记录`)
  } catch (error) {
    apiOnline.value = false
    if (error.status === 401 || error.status === 403) { rows.value = []; total.value = 0; summaryFromApi.value = null; hasLoadedRows.value = true; selectedIds.value = []; notify(error.message, 'error'); return }
    if (!hasLoadedRows.value) {
      summaryFromApi.value = null
      rows.value = fallbackRows.filter(item => (!filters.materialCode || item.materialCode.includes(filters.materialCode)) && (!filters.drugName || item.drugName.includes(filters.drugName)) && (!filters.hospitalId || String(item.hospitalId) === String(filters.hospitalId)))
      total.value = rows.value.length
      notify('后端暂不可用，已展示本地演示数据', 'warning')
    } else notify('后端暂不可用，已保留当前数据，可点击刷新重试', 'warning')
  } finally { loading.value = false }
}
async function loadGoodsGuidance (resetPage = true) {
  currentModule.value = 'goods'; genericModule.value = ''
  if (resetPage) page.value = 1
  selectedIds.value = []
  if (!validateDateRange()) return
  await loadGoodsHospitals()
  loading.value = true
  try {
    const params = queryParams()
  const response = await fetch(`/api/terminal/goods-guidance?${params}`)
    if (!response.ok) { const error = new Error(response.status === 401 ? '未登录，无法查询要货指导' : response.status === 403 ? '没有要货指导数据权限' : '要货指导查询失败'); error.status = response.status; throw error }
    const data = await response.json(); goodsRows.value = data.items || []; goodsLoaded.value = true; total.value = data.total || goodsRows.value.length; goodsSummaryFromApi.value = data.summary || null; apiOnline.value = true; notify(`要货指导查询完成，共 ${data.total || 0} 条记录`)
  } catch (error) {
    apiOnline.value = false
    if (error.status === 401 || error.status === 403) { goodsRows.value = []; total.value = 0; goodsSummaryFromApi.value = null; goodsLoaded.value = true; selectedIds.value = []; notify(error.message, 'error'); return }
    if (!goodsLoaded.value || !goodsRows.value.length) {
      goodsSummaryFromApi.value = null
      goodsRows.value = fallbackRows.map(item => ({ ...item, openingStockParticles: item.actualStockParticles, hospitalSupplyParticles: 0, terminalSalesParticles: item.monthlyAvgConsumption, theoreticalStockParticles: item.actualStockParticles, hospitalTabletQty: item.hospitalShortageParticles, hospitalGainLossQty: item.hospitalLossBags, hospitalGainLossAmount: item.hospitalLossAmount, calculationStatus: item.monthlyAvgConsumption ? 'READY' : 'WARNING', warnings: item.monthlyAvgConsumption ? '' : 'GG_NO_CONSUMPTION' }))
      goodsLoaded.value = true
      total.value = goodsRows.value.length
      notify('后端暂不可用，已展示要货指导演示数据', 'warning')
    } else notify('后端暂不可用，已保留当前数据，可点击刷新重试', 'warning')
  }
  finally { loading.value = false }
}
async function loadStocktake () {
  currentModule.value = 'stocktake'; genericModule.value = ''
  page.value = 1
  selectedIds.value = []
  await search(false)
}
async function loadGenericModule (code) {
  currentModule.value = code; genericModule.value = code; page.value = 1; selectedIds.value = []; loading.value = true
  try { const qs = new URLSearchParams({ page: page.value, pageSize: pageSize.value }); if (filters.hospitalId) qs.set('hospitalId', filters.hospitalId); if (filters.materialCode) qs.set('materialCode', filters.materialCode); if (filters.drugName) qs.set('drugName', filters.drugName); if (filters.startDate) qs.set('startDate', filters.startDate); if (filters.endDate) qs.set('endDate', filters.endDate); const r = await fetch(`/api/terminal/${code}?${qs}`); if (!r.ok) throw new Error('模块数据加载失败'); const d = await r.json(); genericRows.value = d.items || []; genericTotal.value = d.total || 0; genericSummary.value = d.summary || {}; apiOnline.value = true }
  catch (e) { const base = fallbackRows.map((item, index) => ({ id: 10000 + index, moduleCode: code, businessDate: item.summaryDate, startDate: item.guidanceStartDate, endDate: item.guidanceEndDate, openingDate: item.guidanceStartDate, hospitalId: item.hospitalId, hospitalName: item.hospitalName, materialId: 1001 + index, materialCode: item.materialCode, drugName: item.drugName, enterpriseName: '以岭药业', nationalName: item.drugName, particleQty: Number(item.looseParticleQty || item.actualStockParticles || 0), standardParticleQty: Number(item.actualStockParticles || 0), tabletQty: Number(item.hospitalShortageParticles || 0), bagCount: Number(item.bagCount || 0), particleUnitPrice: Number(item.particleUnitPrice || 0), status: 'ACTIVE', validationStatus: 'VALID', lockStatus: 'UNLOCKED', sourceType: 'DEMO' })); genericRows.value = base; genericTotal.value = base.length; genericSummary.value = base.reduce((a, x) => ({ count:(a.count||0)+1, particleQty:(a.particleQty||0)+x.particleQty, standardParticleQty:(a.standardParticleQty||0)+x.standardParticleQty, bagCount:(a.bagCount||0)+x.bagCount }), {}); apiOnline.value = false; notify('后端暂不可用，已展示模块初始数据','warning') } finally { loading.value = false }
}
async function genericSearch () { await loadGenericModule(genericModule.value) }
async function genericCreate () { const drugName=window.prompt('请输入药品/颗粒名称'); if(!drugName) return; const materialCode=window.prompt('请输入物料编码','02.220001'); if(!materialCode) return; try { const r=await fetch(`/api/terminal/${genericModule.value}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({businessDate:new Date().toISOString().slice(0,10),hospitalId:filters.hospitalId||1,hospitalName:hospitalKeyword.value||'河北北方学院附属第一医院',materialCode,drugName,particleQty:0,status:'ACTIVE'})}); if(!r.ok) throw new Error('新增失败'); notify('新增成功'); await loadGenericModule(genericModule.value) } catch(e){notify(e.message,'error')} }
async function loadHospitalsModule () { currentModule.value='hospitals'; genericModule.value=''; loading.value=true; try { const r=await fetch('/api/terminal/hospitals'); if(!r.ok) throw new Error('医院数据加载失败'); hospitalRows.value=await r.json(); apiOnline.value=true } catch(e){hospitalRows.value=[{id:1,name:'河北北方学院附属第一医院',hospitalName:'河北北方学院附属第一医院',hospitalCode:'HB001',categoryCode:'GENERAL',ownerName:'王主任',ownerPhone:'13800000001',level:1,status:'ACTIVE'},{id:2,name:'石家庄市中医院',hospitalName:'石家庄市中医院',hospitalCode:'SJZ001',categoryCode:'TCM',ownerName:'李主任',ownerPhone:'13800000002',level:1,status:'ACTIVE'},{id:3,name:'保定市第一中心医院',hospitalName:'保定市第一中心医院',hospitalCode:'BD001',categoryCode:'GENERAL',ownerName:'赵主任',ownerPhone:'13800000003',level:1,status:'ACTIVE'}];apiOnline.value=false;notify('后端暂不可用，已展示医院初始数据','warning')} finally{loading.value=false} }
async function genericExport () { try { const r = await fetch(`/api/terminal/${genericModule.value}/export`, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({query:{hospitalId:filters.hospitalId||null,materialCode:filters.materialCode||null,drugName:filters.drugName||null,startDate:filters.startDate||null,endDate:filters.endDate||null}}) }); if(!r.ok) throw new Error('导出失败'); await handleExportResponse(r, `${genericMeta[genericModule.value][0]}.csv`, `${genericMeta[genericModule.value][0]}导出完成`) } catch(e){ notify(e.message,'error') } }
async function genericImport () { const text=window.prompt('请粘贴 JSON 数组（例如 [{"materialCode":"02.220001","drugName":"茯苓皮","particleQty":10}]）'); if(!text) return; try { const items=JSON.parse(text); if(!Array.isArray(items)) throw new Error('必须是数组'); const r=await fetch(`/api/terminal/${genericModule.value}/import`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({items})}); if(!r.ok) throw new Error('导入失败'); const d=await r.json(); notify(`导入成功：${d.importedCount||0} 条`); await loadGenericModule(genericModule.value) } catch(e){notify(e.message||'导入失败','error')} }
async function genericAggregate () { try { const r=await fetch(`/api/terminal/${genericModule.value}/aggregate`,{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'}); if(!r.ok) throw new Error('汇总失败'); const d=await r.json(); notify(`汇总完成：${d.successCount||0} 条`); await loadGenericModule(genericModule.value) } catch(e){notify(e.message,'error')} }
async function genericVoid () { if(!selectedIds.value.length) return notify('请先选择记录','warning'); try { const r=await fetch(`/api/terminal/${genericModule.value}`,{method:'DELETE',headers:{'Content-Type':'application/json'},body:JSON.stringify({ids:selectedIds.value})}); if(!r.ok) throw new Error('作废失败'); const d=await r.json(); selectedIds.value=[]; notify(`已作废 ${d.affectedCount||0} 条`); await loadGenericModule(genericModule.value) }catch(e){notify(e.message,'error')} }
async function removeGoodsSelected () {
  if (!selectedIds.value.length) return notify('请先选择需要作废的要货指导记录', 'warning')
  openVoidDialog('goods')
}
function openVoidDialog (kind) {
  const count = selectedIds.value.length
  if (!count) return notify(kind === 'goods' ? '请先选择需要作废的要货指导记录' : '请先选择需要删除的盘点记录', 'warning')
  Object.assign(modal, { visible: true, kind, title: kind === 'goods' ? '作废要货指导记录' : '作废盘点库存记录', message: `将作废选中的 ${count} 条记录，作废后不再出现在有效列表中。`, reason: '来源数据修正，重新汇总', busy: false, count })
}
function closeModal () {
  if (!modal.busy) modal.visible = false
}
async function confirmVoid () {
  const reason = modal.reason.trim()
  if (reason.length < 2 || reason.length > 200) return notify('作废原因必填，长度为 2～200 个字符', 'error')
  modal.busy = true
  try {
    const endpoint = modal.kind === 'goods' ? '/api/terminal/goods-guidance' : '/api/terminal/stocktake'
    const response = await fetch(endpoint, { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ids: selectedIds.value, reason }) })
    if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || '作废失败') }
    const data = await response.json(); selectedIds.value = []; modal.visible = false; notify(`已作废 ${data.affectedCount || 0} 条记录`); modal.kind === 'goods' ? await loadGoodsGuidance(false) : await search(false)
  } catch (error) { notify(error.message || '作废失败，请检查后端服务', 'error') }
  finally { modal.busy = false }
}
async function loadGoodsHospitals () {
  try { const response = await fetch(`/api/terminal/goods-guidance/hospitals${hospitalKeyword.value ? `?keyword=${encodeURIComponent(hospitalKeyword.value)}` : ''}`); if (response.ok) hospitals.value = await response.json() } catch { /* 保留已有选项 */ }
}
async function goodsAggregate () {
  if (loading.value) return
  try { const hasSelection = selectedIds.value.length > 0; const response = await fetch('/api/terminal/goods-guidance/aggregate', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ids: hasSelection ? selectedIds.value : null, query: hasSelection ? null : queryPayload() }) }); if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || '汇总失败') } const data = await response.json(); selectedIds.value = []; notify(`要货指导汇总完成：${data.successCount || 0} 条${data.warningCount ? `，告警 ${data.warningCount} 条` : ''}`); await loadGoodsGuidance(false) } catch (error) { notify(error.message || '要货指导汇总失败，请检查后端服务', 'error') }
}
async function goodsExport () {
  try {
    const response = await fetch('/api/terminal/goods-guidance/export', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: queryPayload(), format: 'XLSX' }) })
    await handleExportResponse(response, '要货指导.xlsx', '要货指导 XLSX 导出完成')
  } catch (error) { notify(error.message || '要货指导导出失败', 'error') }
}
async function handleExportResponse (response, defaultName, successText) {
  if (response.status === 202) {
    const task = await response.json().catch(() => ({}))
    if (!task.taskId) throw new Error('导出任务创建失败')
    notify(`导出任务已创建，预计 ${task.rowCount || 0} 条，正在生成…`, 'warning')
    await pollExportTask(task.taskId, defaultName)
    notify(successText)
    return
  }
  if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || '导出失败') }
  const blob = await response.blob()
  if (!blob.size) throw new Error('导出文件为空')
  downloadBlob(blob, defaultName)
  notify(successText)
}
async function pollExportTask (taskId, defaultName) {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    const response = await fetch(`/api/exports/tasks/${encodeURIComponent(taskId)}`)
    if (!response.ok) throw new Error('导出任务查询失败')
    const task = await response.json()
    if (task.status === 'SUCCESS') {
      const file = await fetch(`/api/exports/tasks/${encodeURIComponent(taskId)}/download`)
      if (!file.ok) { const error = await file.json().catch(() => ({})); throw new Error(error.message || '导出文件下载失败') }
      const blob = await file.blob()
      if (!blob.size) throw new Error('导出文件为空')
      downloadBlob(blob, task.fileName || defaultName)
      return
    }
    if (task.status === 'FAILED') throw new Error(task.failureReason || '异步导出失败')
    await new Promise(resolve => window.setTimeout(resolve, 500))
  }
  throw new Error('导出任务处理超时，请稍后在任务中心查看')
}
function downloadBlob (blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
function reset () { Object.assign(filters, { hospitalId: '', materialCode: '', drugName: '', startDate: '', endDate: '' }); hospitalKeyword.value = ''; sort.field = 'summaryDate'; sort.order = 'desc'; search() }
function validateDateRange () {
  if (!filters.startDate || !filters.endDate) return true
  if (filters.startDate > filters.endDate) { notify('汇总日期开始日期不能晚于结束日期', 'error'); return false }
  const start = new Date(`${filters.startDate}T00:00:00`)
  const end = new Date(`${filters.endDate}T00:00:00`)
  const days = Math.floor((end - start) / 86400000) + 1
  if (days > 366) { notify('日期范围不能超过 366 天', 'error'); return false }
  return true
}
function toggleAll () { selectedIds.value = allSelected.value ? [] : selectableRows.value.map(item => item.id) }
function toggleSort (field) { if (sort.field === field) sort.order = sort.order === 'asc' ? 'desc' : 'asc'; else { sort.field = field; sort.order = 'asc' }; page.value = 1; currentModule.value === 'goods' ? loadGoodsGuidance(false) : search(false) }
async function aggregate () {
  if (loading.value) return
  try {
    const response = await fetch('/api/terminal/stocktake/aggregate', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ids: selectedIds.value, query: selectedIds.value.length ? null : queryPayload() }) })
    if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || '汇总失败') }
    const data = await response.json(); notify(`汇总完成：成功 ${data.successCount || 0} 条`); selectedIds.value = []; await search()
  } catch (error) { notify(error.message || '汇总失败，请检查后端服务和数据配置', 'error') }
}
async function removeSelected () {
  if (!selectedIds.value.length) return notify('请先选择需要删除的盘点记录', 'warning')
  openVoidDialog('stocktake')
}
async function exportData () {
  try {
    const response = await fetch('/api/terminal/stocktake/export', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ query: queryPayload(), format: 'XLSX' }) })
    await handleExportResponse(response, '盘点库存.xlsx', '盘点库存 XLSX 导出完成')
  } catch (error) { notify(error.message || '盘点库存导出失败，请检查后端服务', 'error') }
}
async function openDetail (item) {
  if (!item || !item.id) return
  detailModal.visible = true
  detailModal.loading = true
  detailModal.data = null
  detailModal.error = ''
  try {
    const endpoint = currentModule.value === 'goods' ? '/api/terminal/goods-guidance/' : '/api/terminal/stocktake/'
    const response = await fetch(endpoint + encodeURIComponent(item.id))
    if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || '详情加载失败') }
    detailModal.data = await response.json()
  } catch (error) {
    detailModal.error = error.message || '详情加载失败'
  } finally { detailModal.loading = false }
}
function openSelectedDetail () {
  if (selectedIds.value.length !== 1) return notify('请选择一条记录查看详情', 'warning')
  const item = selectableRows.value.find(row => String(row.id) === String(selectedIds.value[0]))
  openDetail(item)
}
function closeDetail () {
  if (!detailModal.loading) detailModal.visible = false
}
onMounted(() => { loadHospitals(); search() })
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">岭</div>
        <div><strong>以岭药业</strong><small>配方颗粒管理平台</small></div>
      </div>
      <nav class="side-nav">
        <button v-for="item in modules" :key="item.label" class="nav-item" :class="{ active: (item.active && currentModule === 'stocktake') || (item.label === '要货指导' && currentModule === 'goods') || genericModule === ({'医院供货':'hospital-supply','终端销量':'terminal-sales','整袋库存':'whole-inventory','零散库存':'loose-inventory','医院库存':'hospital-inventory','期初库存':'opening-inventory','中药颗粒':'herbal-granules'}[item.label]) || (item.label === '医院管理' && currentModule === 'hospitals') }" type="button" @click="item.label === '要货指导' ? loadGoodsGuidance() : item.active ? loadStocktake() : item.label === '医院管理' ? loadHospitalsModule() : ({'医院供货':'hospital-supply','终端销量':'terminal-sales','整袋库存':'whole-inventory','零散库存':'loose-inventory','医院库存':'hospital-inventory','期初库存':'opening-inventory','中药颗粒':'herbal-granules'}[item.label] ? loadGenericModule({'医院供货':'hospital-supply','终端销量':'terminal-sales','整袋库存':'whole-inventory','零散库存':'loose-inventory','医院库存':'hospital-inventory','期初库存':'opening-inventory','中药颗粒':'herbal-granules'}[item.label]) : undefined)">
          <component :is="item.icon" :size="18" /><span>{{ item.label }}</span><ChevronRight v-if="item.active" class="nav-arrow" :size="14" />
        </button>
        <div class="nav-section">系统功能 <ChevronDown :size="14" /></div>
        <button v-for="item in systemModules" :key="item.label" class="nav-item muted" type="button"><component :is="item.icon" :size="18" /><span>{{ item.label }}</span><ChevronDown v-if="item.label !== '系统日志'" class="nav-arrow" :size="14" /></button>
      </nav>
      <div class="sidebar-footer"><span class="online-dot"></span> 数据服务正常 <span class="version">v1.0.0</span></div>
    </aside>

    <main class="main-content">
      <header class="topbar">
        <div class="breadcrumb"><Menu :size="20" /><span>首页</span><ChevronRight :size="16" /><b>终端管理</b><ChevronRight :size="16" /><strong>{{ currentModule === 'goods' ? '要货指导' : currentModule === 'stocktake' ? '盘点库存' : currentModule === 'hospitals' ? '医院管理' : (genericMeta[currentModule]?.[0] || currentModule) }}</strong></div>
        <div class="top-actions"><Search :size="19" /><span class="message"><ClipboardList :size="17" /><i>1</i></span><SlidersHorizontal :size="19" /><span class="language">文</span><div class="user-avatar">恒安<br>众生</div><ChevronDown :size="14" /></div>
      </header>
      <div class="content-wrap">
        <div class="page-tabs"><span>首页</span><b><ClipboardList :size="14" />{{ currentModule === 'goods' ? '要货指导' : currentModule === 'stocktake' ? '盘点库存' : currentModule === 'hospitals' ? '医院管理' : (genericMeta[currentModule]?.[0] || currentModule) }} <X :size="14" /></b></div>
        <section class="filter-panel card">
          <div class="filter-field hospital-filter"><label>医院名称</label><input v-model="hospitalKeyword" list="hospital-options" placeholder="请选择或搜索医院" @input="searchHospitals" @change="chooseHospital" @keyup.enter="chooseHospital"><datalist id="hospital-options"><option v-for="hospital in hospitals" :key="hospital.id" :value="hospital.hospitalName"></option></datalist></div>
          <div class="filter-field"><label>物料编码</label><input v-model="filters.materialCode" placeholder="请输入物料编码" @keyup.enter="search"></div>
          <div class="filter-field"><label>名称</label><input v-model="filters.drugName" placeholder="请输入药品名称" @keyup.enter="search"></div>
          <div class="filter-field date-filter"><label>汇总日期</label><input v-model="filters.startDate" type="date"><span>至</span><input v-model="filters.endDate" type="date"></div>
          <button class="btn primary" type="button" @click="search"><Search :size="16" />搜索</button><button class="btn ghost" type="button" @click="reset"><RefreshCw :size="15" />重置</button>
        </section>

        <section v-if="genericModule" class="table-card card">
          <div class="toolbar"><div class="toolbar-left"><button class="btn primary" @click="genericCreate"><Package :size="15"/>新增</button><button class="btn ghost" @click="genericImport"><Download :size="15"/>导入</button><button class="btn danger-outline" :disabled="!selectedIds.length" @click="genericVoid"><Trash2 :size="15"/>作废</button><button class="btn orange-outline" @click="genericExport"><Download :size="15"/>导出</button><button class="btn orange-outline" @click="genericAggregate"><TrendingDown :size="15"/>汇总</button></div><div class="toolbar-right"><span class="api-status" :class="{online:apiOnline}"><span></span>{{apiOnline?'已连接数据库':'离线'}}</span><button class="circle-btn" @click="genericSearch"><RefreshCw :size="16"/></button></div></div>
          <div class="table-scroll"><table class="stock-table"><thead><tr class="column-head"><th><input type="checkbox" :checked="genericRows.length && selectedIds.length===genericRows.length" @change="selectedIds = selectedIds.length===genericRows.length ? [] : genericRows.map(x=>x.id)"></th><th>ID</th><th>日期</th><th>医院</th><th>物料编码</th><th>药品名称</th><th>颗粒量</th><th>标准颗粒量</th><th>袋数</th><th>状态</th><th>校验状态</th></tr></thead><tbody><tr v-for="item in genericRows" :key="item.id"><td><input type="checkbox" :value="item.id" v-model="selectedIds"></td><td>{{item.id}}</td><td>{{item.businessDate||item.startDate||item.openingDate||'—'}}</td><td>{{item.hospitalName||'—'}}</td><td>{{item.materialCode||'—'}}</td><td>{{item.drugName||item.enterpriseName||'—'}}</td><td>{{display(item.particleQty)}}</td><td>{{display(item.standardParticleQty)}}</td><td>{{display(item.bagCount)}}</td><td>{{item.status}}</td><td>{{item.validationStatus}}</td></tr><tr v-if="!genericRows.length"><td colspan="11" class="empty"><Package :size="30"/>暂无数据</td></tr></tbody><tfoot><tr><td></td><td class="total-label">统计汇总</td><td colspan="4"></td><td>{{display(genericSummary.particleQty)}}</td><td>{{display(genericSummary.standardParticleQty)}}</td><td>{{display(genericSummary.bagCount)}}</td><td colspan="2"></td></tr></tfoot></table></div>
          <div class="table-footer"><span>共 {{genericTotal}} 条</span><span>每页 <select v-model.number="pageSize" @change="genericSearch"><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option></select> 条</span></div>
        </section>
        <section v-else-if="currentModule === 'goods'" class="table-card card">
          <div class="toolbar"><div class="toolbar-left"><button class="btn danger-outline" type="button" :disabled="!selectedIds.length || loading" @click="removeGoodsSelected"><Trash2 :size="15" />作废<span v-if="selectedIds.length">({{ selectedIds.length }})</span></button><button class="btn ghost" type="button" :disabled="selectedIds.length !== 1 || loading" @click="openSelectedDetail"><Eye :size="15" />详情</button><button class="btn orange-outline" type="button" :disabled="loading" @click="goodsExport"><Download :size="15" />导出</button><button class="btn orange-outline" type="button" :disabled="loading" @click="goodsAggregate"><TrendingDown :size="15" />汇总</button></div><div class="toolbar-right"><span class="api-status" :class="{ online: apiOnline }"><span></span>{{ apiOnline ? '已连接数据库' : '演示数据' }}</span><button class="circle-btn" type="button" :disabled="loading" @click="loadGoodsGuidance"><RefreshCw :size="16" /></button></div></div>
          <div class="table-scroll"><table class="stock-table guidance-table"><thead><tr class="group-head"><th rowspan="2" class="check-col"><input type="checkbox" :checked="allSelected" @change="toggleAll"></th><th colspan="4" class="group basic">基础数据</th><th colspan="4" class="group bag">库存链路</th><th colspan="2" class="group hospital">医院库存</th><th colspan="3" class="group result">亏涨结果</th><th colspan="5" class="group guidance">要货指导</th></tr><tr class="column-head"><th @click="toggleSort('summaryDate')">汇总日期 <span>↕</span></th><th @click="toggleSort('hospitalName')">医院名称 <span>↕</span></th><th @click="toggleSort('materialCode')">物料编码 <span>↕</span></th><th @click="toggleSort('drugName')">药品名称 <span>↕</span></th><th @click="toggleSort('openingStockParticles')">期初库存<br>(颗粒) <span>↕</span></th><th>医院供货<br>(颗粒)</th><th @click="toggleSort('terminalSalesParticles')">终端销量<br>(颗粒) <span>↕</span></th><th @click="toggleSort('theoreticalStockParticles')">理论库存<br>(颗粒) <span>↕</span></th><th @click="toggleSort('hospitalTabletQty')">饮片量 <span>↕</span></th><th @click="toggleSort('hospitalParticleQty')">颗粒量 <span>↕</span></th><th @click="toggleSort('hospitalGainLossQty')">亏涨(袋) <span>↕</span></th><th @click="toggleSort('particleUnitPrice')">颗粒单价 <span>↕</span></th><th @click="toggleSort('hospitalGainLossAmount')">亏涨金额 <span>↕</span></th><th>指导范围</th><th @click="toggleSort('monthlyAvgConsumption')">月均消耗<br>(颗粒) <span>↕</span></th><th @click="toggleSort('currentMonthDemandBags')">本月需求量<br>(袋) <span>↕</span></th><th @click="toggleSort('availableMonths')">可用时长<br>(月) <span>↕</span></th><th @click="toggleSort('thirtyDayPurchaseBags')">30天需进货量<br>(袋) <span>↕</span></th></tr></thead><tbody><tr v-for="item in goodsRows" :key="item.id" :class="{ selected: selectedIds.includes(item.id) }"><td class="check-col"><input type="checkbox" :value="item.id" v-model="selectedIds"></td><td>{{ item.summaryDate }}</td><td class="ellipsis" :title="item.hospitalName">{{ item.hospitalName }}</td><td>{{ item.materialCode }}</td><td class="drug" :title="item.warnings || ''">{{ item.drugName }}<span v-if="item.warnings" class="warning-mark" title="存在计算告警">!</span></td><td class="number blue">{{ display(item.openingStockParticles) }}</td><td class="number">{{ display(item.hospitalSupplyParticles) }}</td><td class="number">{{ display(item.terminalSalesParticles) }}</td><td class="number blue">{{ display(item.theoreticalStockParticles) }}</td><td>{{ display(item.hospitalTabletQty) }}</td><td class="number blue">{{ display(item.hospitalParticleQty) }}</td><td class="number red">{{ display(item.hospitalGainLossQty) }}</td><td>{{ displayPrice(item.particleUnitPrice) }}</td><td class="number red">{{ displayAmount(item.hospitalGainLossAmount) }}</td><td class="range">{{ item.guidanceStartDate }} 至 {{ item.guidanceEndDate }}</td><td>{{ display(item.monthlyAvgConsumption) }}</td><td>{{ display(item.currentMonthDemandBags) }}</td><td :class="{ warning: item.availableMonths < 1 }">{{ display(item.availableMonths) }}</td><td>{{ display(item.thirtyDayPurchaseBags) }}</td></tr><tr v-if="!goodsRows.length"><td colspan="19" class="empty"><Package :size="30" />暂无要货指导数据</td></tr></tbody><tfoot><tr><td></td><td class="total-label">统计汇总</td><td colspan="3"></td><td>{{ display(goodsSummary.openingStockParticles) }}</td><td>{{ display(goodsSummary.hospitalSupplyParticles) }}</td><td>{{ display(goodsSummary.terminalSalesParticles) }}</td><td>{{ display(goodsSummary.theoreticalStockParticles) }}</td><td>{{ display(goodsSummary.hospitalTabletQty) }}</td><td>{{ display(goodsSummary.hospitalParticleQty) }}</td><td class="red">{{ display(goodsSummary.hospitalGainLossQty) }}</td><td></td><td class="red">{{ displayAmount(goodsSummary.hospitalGainLossAmount) }}</td><td></td><td>{{ display(goodsSummary.monthlyAvgConsumption) }}</td><td>{{ display(goodsSummary.currentMonthDemandBags) }}</td><td></td><td>{{ display(goodsSummary.thirtyDayPurchaseBags) }}</td></tr></tfoot></table></div>
          <div class="table-footer"><span>共 {{ total || goodsRows.length }} 条</span><span>每页 <select v-model.number="pageSize" @change="loadGoodsGuidance"><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option><option :value="200">200</option></select> 条</span><button class="page-btn" :disabled="page <= 1 || loading" @click="page--; loadGoodsGuidance(false)"><ChevronLeft :size="16" /></button><span class="page-current">{{ page }}</span><button class="page-btn" :disabled="page >= pageCount || loading" @click="page++; loadGoodsGuidance(false)"><ChevronRight :size="16" /></button></div>
        </section>
        <section v-else-if="currentModule === 'hospitals'" class="table-card card">
          <div class="toolbar"><div class="toolbar-left"><button class="btn primary" @click="loadHospitalsModule"><RefreshCw :size="15"/>刷新医院树</button></div><div class="toolbar-right"><span class="api-status" :class="{online:apiOnline}"><span></span>{{apiOnline?'已连接数据库':'离线'}}</span></div></div>
          <div class="table-scroll"><table class="stock-table"><thead><tr class="column-head"><th>ID</th><th>医院名称</th><th>编码</th><th>类别</th><th>负责人</th><th>联系电话</th><th>层级</th><th>状态</th></tr></thead><tbody><tr v-for="item in hospitalRows" :key="item.id"><td>{{item.id}}</td><td>{{item.name||item.hospitalName}}</td><td>{{item.hospitalCode||'—'}}</td><td>{{item.categoryCode||'—'}}</td><td>{{item.ownerName||'—'}}</td><td>{{item.ownerPhone||'—'}}</td><td>{{item.level||1}}</td><td>{{item.status}}</td></tr><tr v-if="!hospitalRows.length"><td colspan="8" class="empty"><Building2 :size="30"/>暂无医院数据</td></tr></tbody></table></div>
        </section>
        <section v-else-if="currentModule === 'stocktake'" class="table-card card">
          <div class="toolbar"><div class="toolbar-left"><button class="btn danger-outline" type="button" :disabled="!selectedIds.length || loading" @click="removeSelected"><Trash2 :size="15" />删除<span v-if="selectedIds.length">({{ selectedIds.length }})</span></button><button class="btn ghost" type="button" :disabled="selectedIds.length !== 1 || loading" @click="openSelectedDetail"><Eye :size="15" />详情</button><button class="btn orange-outline" type="button" :disabled="loading" @click="exportData"><Download :size="15" />导出</button><button class="btn orange-outline" type="button" :disabled="loading" @click="aggregate"><TrendingDown :size="15" />汇总</button></div><div class="toolbar-right"><span class="api-status" :class="{ online: apiOnline }"><span></span>{{ apiOnline ? '已连接数据库' : '演示数据' }}</span><button class="circle-btn" type="button" title="刷新" :disabled="loading" @click="search"><RefreshCw :size="16" /></button><button class="circle-btn" type="button" title="筛选"><Filter :size="16" /></button></div></div>
          <div class="table-scroll"><table class="stock-table"><colgroup><col v-for="index in 22" :key="index"></col></colgroup><thead><tr class="group-head"><th rowspan="2" class="check-col"><input type="checkbox" :checked="allSelected" @change="toggleAll"></th><th colspan="5" class="group basic">基础数据</th><th colspan="2" class="group bag">整袋库存</th><th colspan="1" class="group loose">零散库存</th><th rowspan="2" class="actual">实际库存<br><small>(颗粒量)</small></th><th colspan="2" class="group hospital">医院库存</th><th colspan="5" class="group result">亏涨结果</th><th colspan="5" class="group guidance">要货指导</th></tr><tr class="column-head"><th @click="toggleSort('summaryDate')">汇总日期 <span>↕</span></th><th @click="toggleSort('hospitalName')">医院名称 <span>↕</span></th><th @click="toggleSort('materialCode')">物料编码 <span>↕</span></th><th @click="toggleSort('drugName')">药品名称 <span>↕</span></th><th>状态</th><th @click="toggleSort('bagCount')">袋数 <span>↕</span></th><th @click="toggleSort('wholeParticleQty')">颗粒量 <span>↕</span></th><th @click="toggleSort('looseParticleQty')">颗粒量 <span>↕</span></th><th @click="toggleSort('hospitalShortageParticles')">欠片量 <span>↕</span></th><th @click="toggleSort('hospitalParticleQty')">颗粒量 <span>↕</span></th><th @click="toggleSort('factoryIncreaseBags')">药厂涨(袋) <span>↕</span></th><th @click="toggleSort('hospitalLossBags')">医院亏(袋) <span>↕</span></th><th @click="toggleSort('particleUnitPrice')">颗粒单价 <span>↕</span></th><th @click="toggleSort('hospitalLossAmount')">医院亏金额 <span>↕</span></th><th @click="toggleSort('factoryIncreaseAmount')">药厂涨金额 <span>↕</span></th><th>要货指导范围</th><th @click="toggleSort('monthlyAvgConsumption')">月均消耗<br><small>(颗粒量)</small> <span>↕</span></th><th @click="toggleSort('currentMonthDemandBags')">本月需求量<br><small>(袋)</small> <span>↕</span></th><th @click="toggleSort('availableMonths')">现场剩余可用<br>时长(月) <span>↕</span></th><th @click="toggleSort('thirtyDayPurchaseBags')">30天需进货量<br>(袋) <span>↕</span></th></tr></thead><tbody><tr v-for="item in visibleRows" :key="item.id" :class="{ selected: selectedIds.includes(item.id) }"><td class="check-col"><input type="checkbox" :value="item.id" v-model="selectedIds"></td><td>{{ item.summaryDate }}</td><td class="ellipsis" :title="item.hospitalName">{{ item.hospitalName }}</td><td class="ellipsis" :title="item.materialCode">{{ item.materialCode }}</td><td class="drug">{{ item.drugName }}</td><td><span class="status-pill" :class="{ void: item.status === 'VOID' }">{{ statusLabel(item) }}</span></td><td class="number blue">{{ display(item.bagCount) }}</td><td class="number blue">{{ display(item.wholeParticleQty) }}</td><td class="number">{{ display(item.looseParticleQty) }}</td><td class="number blue">{{ display(item.actualStockParticles) }}</td><td class="number">{{ display(item.hospitalShortageParticles) }}</td><td class="number blue">{{ display(item.hospitalParticleQty) }}</td><td class="number" :class="{ red: item.factoryIncreaseBags < 0 }">{{ display(item.factoryIncreaseBags) }}</td><td class="number red">{{ display(item.hospitalLossBags) }}</td><td class="number">{{ displayPrice(item.particleUnitPrice) }}</td><td class="number red">{{ displayAmount(item.hospitalLossAmount) }}</td><td class="number">{{ display(item.factoryIncreaseAmount) }}</td><td class="range">{{ item.guidanceStartDate }} 至 {{ item.guidanceEndDate }}</td><td class="number">{{ display(item.monthlyAvgConsumption) }}</td><td class="number">{{ display(item.currentMonthDemandBags) }}</td><td class="number" :class="{ warning: item.availableMonths < 1 }">{{ display(item.availableMonths) }}</td><td class="number">{{ display(item.thirtyDayPurchaseBags) }}</td></tr><tr v-if="!visibleRows.length"><td colspan="22" class="empty"><Package :size="30" />暂无数据</td></tr></tbody><tfoot><tr><td></td><td class="total-label">统计汇总</td><td colspan="4"></td><td class="number">{{ display(summary.bagCount) }}</td><td class="number">{{ display(summary.wholeParticleQty) }}</td><td class="number">{{ display(summary.looseParticleQty) }}</td><td class="number">{{ display(summary.actualStockParticles) }}</td><td class="number">{{ display(summary.hospitalShortageParticles) }}</td><td class="number">{{ display(summary.hospitalParticleQty) }}</td><td class="number">{{ display(summary.factoryIncreaseBags) }}</td><td class="number red">{{ display(summary.hospitalLossBags) }}</td><td></td><td class="number red">{{ displayAmount(summary.hospitalLossAmount) }}</td><td class="number">{{ displayAmount(summary.factoryIncreaseAmount) }}</td><td></td><td class="number">{{ display(summary.monthlyAvgConsumption) }}</td><td class="number">{{ display(summary.currentMonthDemandBags) }}</td><td></td><td class="number">{{ display(summary.thirtyDayPurchaseBags) }}</td></tr></tfoot></table></div>
          <div class="table-footer"><span>共 {{ total || visibleRows.length }} 条</span><span>每页 <select v-model.number="pageSize" @change="search"><option :value="20">20</option><option :value="50">50</option><option :value="100">100</option><option :value="200">200</option></select> 条</span><button class="page-btn" :disabled="page <= 1" @click="page--; search(false)"><ChevronLeft :size="16" /></button><span class="page-current">{{ page }}</span><button class="page-btn" :disabled="page >= pageCount" @click="page++; search(false)"><ChevronRight :size="16" /></button></div>
        </section>
        <footer class="page-footer">© 2026 以岭药业 · 配方颗粒终端管理系统 <span>数据更新时间：2025-12-26</span></footer>
      </div>
    </main>
    <Transition name="toast"><div v-if="notice.text" class="toast" :class="notice.type"><CheckCircle2 v-if="notice.type === 'success'" :size="17" /><AlertTriangle v-else :size="17" />{{ notice.text }}</div></Transition>
    <div v-if="modal.visible" class="modal-mask" @click.self="closeModal">
      <section class="modal-card" role="dialog" aria-modal="true" :aria-label="modal.title">
        <div class="modal-header"><strong>{{ modal.title }}</strong><button class="modal-close" type="button" :disabled="modal.busy" @click="closeModal"><X :size="17" /></button></div>
        <p class="modal-message">{{ modal.message }}</p>
        <label class="modal-label" for="void-reason">作废原因 <span>（2～200个字符）</span></label>
        <textarea id="void-reason" v-model="modal.reason" :disabled="modal.busy" maxlength="200" rows="3" placeholder="请输入作废原因"></textarea>
        <div class="modal-footer"><button class="btn ghost" type="button" :disabled="modal.busy" @click="closeModal">取消</button><button class="btn danger-solid" type="button" :disabled="modal.busy" @click="confirmVoid">{{ modal.busy ? '提交中…' : '确认作废' }}</button></div>
      </section>
    </div>
    <div v-if="detailModal.visible" class="modal-mask" @click.self="closeDetail">
      <section class="modal-card detail-card" role="dialog" aria-modal="true" aria-label="库存详情">
        <div class="modal-header"><strong>{{ currentModule === 'goods' ? '要货指导详情' : '盘点库存详情' }}</strong><button class="modal-close" type="button" :disabled="detailModal.loading" @click="closeDetail"><X :size="17" /></button></div>
        <div v-if="detailModal.loading" class="detail-loading">正在加载详情…</div>
        <div v-else-if="detailModal.error" class="detail-error">{{ detailModal.error }}</div>
        <div v-else-if="detailModal.data" class="detail-body">
          <div class="detail-grid"><div><span>记录 ID</span><b>{{ detailModal.data.item.id }}</b></div><div><span>汇总日期</span><b>{{ detailModal.data.item.summaryDate }}</b></div><div><span>医院</span><b>{{ detailModal.data.item.hospitalName }}</b></div><div><span>药品</span><b>{{ detailModal.data.item.drugName }}</b></div><div><span>物料编码</span><b>{{ detailModal.data.item.materialCode }}</b></div><div><span>规则版本</span><b>{{ detailModal.data.calculationVersion || '—' }}</b></div></div>
          <template v-if="currentModule === 'goods'"><h4>来源追溯</h4><div class="detail-grid"><div><span>期初来源</span><b>{{ detailModal.data.item.openingStockSource || '—' }}</b></div><div><span>期初同步时间</span><b>{{ detailModal.data.sourceAsOf?.openingStock || '—' }}</b></div><div><span>医院供货来源</span><b>{{ detailModal.data.item.hospitalSupplySource || '—' }}</b></div><div><span>供货同步时间</span><b>{{ detailModal.data.sourceAsOf?.hospitalSupply || '—' }}</b></div><div><span>终端销量来源</span><b>{{ detailModal.data.item.terminalSalesSource || '—' }}</b></div><div><span>销量同步时间</span><b>{{ detailModal.data.sourceAsOf?.terminalSales || '—' }}</b></div><div><span>有效消耗天数</span><b>{{ detailModal.data.item.validConsumptionDays }}</b></div><div><span>缺失消耗天数</span><b>{{ detailModal.data.item.missingConsumptionDays }}</b></div></div></template>
          <h4>计算公式</h4><div class="formula-list"><div v-for="(formula, name) in detailModal.data.formula" :key="name"><span>{{ name }}</span><code>{{ formula }}</code></div></div>
          <h4>操作记录</h4><div v-if="detailModal.data.auditLogs?.length" class="audit-list"><div v-for="log in detailModal.data.auditLogs" :key="log.id"><b>{{ log.operationType }}</b><span>{{ log.result }}</span><small>{{ log.createdAt }}</small></div></div><p v-else class="detail-muted">暂无操作记录</p>
        </div>
      </section>
    </div>
  </div>
</template>

