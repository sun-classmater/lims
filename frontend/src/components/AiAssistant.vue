<template>
  <div class="ai-assistant">
    <transition name="ai-pop">
      <section
        v-if="open"
        class="ai-panel"
        :style="panelStyle"
      >
        <header class="ai-header">
          <div class="ai-avatar">
            <el-icon><MagicStick /></el-icon>
          </div>
          <div class="ai-head-text">
            <strong>LIMS 智能助手</strong>
            <small>{{ statusText }}</small>
          </div>
          <button
            class="ai-icon-btn"
            :title="voiceEnabled ? '关闭语音播报' : '开启语音播报'"
            @click="toggleVoice"
          >
            <el-icon>
              <component :is="voiceEnabled ? Microphone : Mute" />
            </el-icon>
          </button>
          <button
            class="ai-icon-btn"
            title="关闭"
            @click="open = false"
          >
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <div
          ref="listRef"
          class="ai-messages"
        >
          <div
            v-for="message in messages"
            :key="message.id"
            class="ai-msg"
            :class="message.role"
          >
            <div class="ai-bubble">
              {{ message.text }}
            </div>
          </div>
          <div
            v-if="thinking"
            class="ai-msg assistant"
          >
            <div class="ai-bubble ai-typing">
              <span />
              <span />
              <span />
            </div>
          </div>
        </div>

        <div
          v-if="messages.length <= 1"
          class="ai-chips"
        >
          <button
            v-for="chip in chips"
            :key="chip"
            type="button"
            @click="send(chip)"
          >
            {{ chip }}
          </button>
        </div>

        <footer class="ai-input">
          <input
            v-model="input"
            :disabled="thinking"
            placeholder="输入问题，或点麦克风说话…"
            @keyup.enter="send()"
          >
          <button
            class="ai-mic"
            :class="{ recording }"
            :title="recording ? '停止录音' : '语音输入'"
            type="button"
            @click="toggleMic"
          >
            <el-icon><Microphone /></el-icon>
          </button>
          <button
            class="ai-send"
            :disabled="!input.trim() || thinking"
            type="button"
            @click="send()"
          >
            <el-icon><Promotion /></el-icon>
          </button>
        </footer>
      </section>
    </transition>

    <div
      class="ai-ball"
      :class="{ dragging, open }"
      :style="{ left: `${pos.x}px`, top: `${pos.y}px` }"
      title="LIMS 智能助手"
      @pointerdown="onPointerDown"
    >
      <span class="ai-ball-ring" />
      <el-icon><MagicStick /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Close,
  MagicStick,
  Microphone,
  Mute,
  Promotion,
} from '@element-plus/icons-vue'

import { deleteData, fetchData, updateData } from '@/api/data'
import { openProjectRegisterDialog } from '@/utils/entityDialogs'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  text: string
}

interface SpeechRecognitionResultLike {
  transcript: string
}

interface SpeechRecognitionEventLike {
  results: ArrayLike<ArrayLike<SpeechRecognitionResultLike>>
}

interface SpeechRecognitionLike {
  lang: string
  continuous: boolean
  interimResults: boolean
  onresult: ((event: SpeechRecognitionEventLike) => void) | null
  onerror: (() => void) | null
  onend: (() => void) | null
  start: () => void
  stop: () => void
}

type SpeechRecognitionCtor = new () => SpeechRecognitionLike

const BALL_SIZE = 56
const router = useRouter()

const open = ref(false)
const input = ref('')
const thinking = ref(false)
const recording = ref(false)
const dragging = ref(false)
const messages = ref<ChatMessage[]>([
  {
    id: 1,
    role: 'assistant',
    text: '你好，我是 LIMS 智能助手。你可以让我打开页面、查询样品、统计待办，也可以直接和我聊天。',
  },
])
const listRef = ref<HTMLElement>()
const pos = reactive({ x: 0, y: 0 })
const voiceEnabled = ref(localStorage.getItem('lims_ai_voice') !== 'off')

let sequence = 1
let startX = 0
let startY = 0
let originX = 0
let originY = 0
let moved = false
let recognition: SpeechRecognitionLike | null = null
let lastProjectCode = ''

const chips = [
  '你能做什么',
  '待安排有多少',
  '打开数据录入',
  '查样品 JK(2023)-SA-182',
]

const speechWindow = window as unknown as {
  SpeechRecognition?: SpeechRecognitionCtor
  webkitSpeechRecognition?: SpeechRecognitionCtor
}
const speechRecognitionCtor =
  speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition

const statusText = computed(() => {
  if (recording.value) {
    return '正在听你说话…'
  }
  return thinking.value ? '正在思考…' : '在线 · 可文字或语音'
})

const panelStyle = computed(() => {
  const width = Math.min(370, window.innerWidth - 24)
  const height = Math.min(540, window.innerHeight - 90)
  let left = pos.x + BALL_SIZE + 12
  if (left + width > window.innerWidth - 12) {
    left = pos.x - width - 12
  }
  left = Math.max(12, left)
  const top = Math.min(
    Math.max(12, pos.y + BALL_SIZE - height),
    window.innerHeight - height - 12,
  )
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`,
  }
})

function clampPosition() {
  pos.x = Math.min(Math.max(8, pos.x), window.innerWidth - BALL_SIZE - 8)
  pos.y = Math.min(Math.max(8, pos.y), window.innerHeight - BALL_SIZE - 8)
}

function onPointerDown(event: PointerEvent) {
  if (event.button !== 0) {
    return
  }
  dragging.value = true
  moved = false
  startX = event.clientX
  startY = event.clientY
  originX = pos.x
  originY = pos.y
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

function onPointerMove(event: PointerEvent) {
  if (!dragging.value) {
    return
  }
  const dx = event.clientX - startX
  const dy = event.clientY - startY
  if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
    moved = true
  }
  pos.x = originX + dx
  pos.y = originY + dy
  clampPosition()
}

function onPointerUp() {
  dragging.value = false
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  localStorage.setItem('lims_ai_pos', JSON.stringify({ x: pos.x, y: pos.y }))
  if (!moved) {
    open.value = !open.value
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}

async function send(text?: string) {
  const content = (text ?? input.value).trim()
  if (!content || thinking.value) {
    return
  }
  input.value = ''
  messages.value.push({ id: ++sequence, role: 'user', text: content })
  scrollToBottom()
  thinking.value = true
  try {
    const reply = await replyTo(content)
    messages.value.push({ id: ++sequence, role: 'assistant', text: reply })
    speak(reply)
  } catch (error) {
    const message = error instanceof Error ? error.message : '处理失败'
    messages.value.push({
      id: ++sequence,
      role: 'assistant',
      text: `处理时出了点问题：${message}`,
    })
  } finally {
    thinking.value = false
    scrollToBottom()
  }
}

function speak(text: string) {
  if (!voiceEnabled.value || !('speechSynthesis' in window)) {
    return
  }
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text.replace(/[「」]/g, ''))
  utterance.lang = 'zh-CN'
  utterance.rate = 1
  utterance.pitch = 1
  window.speechSynthesis.speak(utterance)
}

function toggleVoice() {
  voiceEnabled.value = !voiceEnabled.value
  localStorage.setItem('lims_ai_voice', voiceEnabled.value ? 'on' : 'off')
  if (!voiceEnabled.value) {
    window.speechSynthesis?.cancel()
  }
}

function toggleMic() {
  if (!speechRecognitionCtor) {
    messages.value.push({
      id: ++sequence,
      role: 'assistant',
      text: '当前浏览器不支持语音识别，建议使用 Edge 或 Chrome 浏览器。',
    })
    scrollToBottom()
    return
  }
  if (recording.value) {
    recognition?.stop()
    return
  }

  recognition = new speechRecognitionCtor()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = false
  recognition.onresult = (event) => {
    const transcript = event.results?.[0]?.[0]?.transcript ?? ''
    if (transcript.trim()) {
      void send(transcript.trim())
    }
  }
  recognition.onerror = () => {
    recording.value = false
    messages.value.push({
      id: ++sequence,
      role: 'assistant',
      text: '语音识别没有成功，请再试一次。',
    })
    scrollToBottom()
  }
  recognition.onend = () => {
    recording.value = false
  }
  recording.value = true
  recognition.start()
}

/* ---------------- 意图处理 ---------------- */

const pageMap: Array<{ keys: string[]; path: string; title: string }> = [
  { keys: ['中心文件资料', '文件资料'], path: '/service/files', title: '中心文件资料' },
  { keys: ['项目库查询'], path: '/service/project-library', title: '项目库查询' },
  { keys: ['在检项目', '在检'], path: '/service/current', title: '在检项目查询' },
  { keys: ['历史项目', '历史'], path: '/service/history', title: '历史项目查询' },
  { keys: ['监抽任务'], path: '/task/supervision-manage', title: '监抽任务管理' },
  { keys: ['监抽执行'], path: '/task/supervision-execute', title: '监抽执行查询' },
  { keys: ['监抽抽样'], path: '/task/supervision-sample', title: '监抽抽样查询' },
  { keys: ['导入采样单', '采样单'], path: '/sample/import', title: '导入采样单' },
  { keys: ['样品登记确认', '登记确认'], path: '/sample/register', title: '样品登记确认' },
  { keys: ['报告下载'], path: '/sample/report-download', title: '报告下载' },
  { keys: ['登记查询'], path: '/sample/register-query', title: '登记查询' },
  { keys: ['导出数据', '导出'], path: '/sample/export', title: '导出数据' },
  { keys: ['单个样品分解', '单个分解'], path: '/dispatch/single', title: '单个样品分解' },
  { keys: ['批量样品分解', '批量分解'], path: '/dispatch/batch', title: '批量样品分解' },
  { keys: ['任务安排'], path: '/dispatch/arrange', title: '任务安排' },
  { keys: ['批量安排'], path: '/dispatch/batch-arrange', title: '批量安排' },
  { keys: ['任务查询'], path: '/inspection/query', title: '任务查询' },
  { keys: ['数据录入', '录入'], path: '/inspection/input', title: '数据录入' },
  { keys: ['报告签发', '签发'], path: '/review/issue', title: '报告签发' },
  { keys: ['检验汇总', '汇总'], path: '/review/query', title: '检验汇总查询' },
  { keys: ['用户信息'], path: '/base/user', title: '用户信息' },
  { keys: ['项目库维护'], path: '/base/project', title: '项目库维护' },
  { keys: ['中心资料上传', '资料上传'], path: '/base/upload', title: '中心资料上传' },
]

const HELP_TEXT =
  '我可以做这些事：\n' +
  '· 打开页面，例如“打开数据录入”“去报告下载”\n' +
  '· 查询样品，例如“查样品 JK(2023)-SA-182”\n' +
  '· 统计待办，例如“待安排有多少”“待签发有多少”\n' +
  '· 修改备注，例如“把 JK(2023)-SA-182 的备注改成已核对”\n' +
  '· 删除数据，例如“删除 JK(2023)-SA-182”（会二次确认）\n' +
  '也可以直接和我聊天，点麦克风还能用语音说。'

async function replyTo(text: string): Promise<string> {
  const message = text.trim()
  if (!message) {
    return ''
  }

  if (/(你好|您好|hi|hello)/i.test(message) && message.length <= 12) {
    return '你好！我是 LIMS 智能助手，可以帮你打开页面、查询样品、统计待办，也可以陪你聊两句。'
  }
  if (/(你是谁|能做什么|有什么功能|帮助|help)/i.test(message)) {
    return HELP_TEXT
  }
  if (/(谢谢|感谢)/.test(message)) {
    return '不客气，随时叫我。'
  }

  if (/^(打开|查看|打开页面|查看详情|看看)$/.test(message) && lastProjectCode) {
    await openProjectRegisterDialog({ bh: lastProjectCode })
    return `已经打开样品 ${lastProjectCode} 的登记信息窗口。`
  }

  if (/(打开|进入|跳转|去|看看)/.test(message)) {
    const page = pageMap.find((item) => item.keys.some((key) => message.includes(key)))
    if (page) {
      await router.push(page.path)
      return `已经帮你打开「${page.title}」。`
    }
    if (/(页面|详情|信息|资料)/.test(message)) {
      if (lastProjectCode) {
        await openProjectRegisterDialog({ bh: lastProjectCode })
        return `已经打开样品 ${lastProjectCode} 的登记信息窗口。`
      }
      return '你想打开哪个页面？可以说“打开数据录入”“打开报告下载”“打开在检项目查询”等。'
    }
  }

  if (/(多少|统计|数量|几个|几条)/.test(message)) {
    const countReply = await countReplyFor(message)
    if (countReply) {
      return countReply
    }
  }

  const update = message.match(
    /(?:修改|改|更新)(?:样品|项目)?\s*(JK\(\d{4}\)-[A-Za-z0-9]+-\d+)\s*(?:的)?\s*备注\s*(?:为|成|改成)\s*(.+)$/i,
  )
  if (update) {
    return updateRemark(update[1], update[2].trim())
  }

  const remove = message.match(
    /(?:删除|删掉)\s*(?:样品|项目)?\s*(JK\(\d{4}\)-[A-Za-z0-9]+-\d+)/i,
  )
  if (remove) {
    return removeProject(remove[1])
  }

  const code = message.match(/JK\(\d{4}\)-[A-Za-z0-9]+-\d+/i)
  if (code) {
    return describeProject(code[0])
  }

  if (/(样品|项目|任务)/.test(message)) {
    return '可以告诉我样品编号，例如“查一下 JK(2023)-SA-182”，我会把样品名称、受检单位、结论和日期告诉你。'
  }

  return chatReply(message)
}

async function countReplyFor(message: string): Promise<string | null> {
  if (/待登记/.test(message)) {
    return `当前待登记样品 ${await count('project', { state_register: 'F' })} 个。`
  }
  if (/待分解/.test(message)) {
    return `当前待分解样品 ${await count('project', { state_divNe: 'T' })} 个。`
  }
  if (/待安排/.test(message)) {
    return `当前待安排样品 ${await count('project', {
      state_div: 'T',
      state_arrangeNe: 'T',
    })} 个。`
  }
  if (/待签发|待授权/.test(message)) {
    return `当前待签发项目 ${await count('project', {
      state_comlete: 'T',
      state_verifyNe: 'T',
    })} 个。`
  }
  if (/待检验/.test(message)) {
    return `当前待检验任务 ${await count('task', { state_jyyNe: 'T' })} 条。`
  }
  if (/在检/.test(message)) {
    return `当前在检项目 ${await count('project', { state_comleteNe: 'T' })} 个。`
  }
  if (/(样品|项目)/.test(message)) {
    return `系统内共有样品项目 ${await count('project', {})} 个。`
  }
  return null
}

async function count(
  entity: string,
  filters: Record<string, string | number>,
): Promise<number> {
  const result = await fetchData(entity, { page: 1, size: 1, ...filters })
  return result.total
}

async function findProject(code: string): Promise<Record<string, unknown> | null> {
  const result = await fetchData('project', { page: 1, size: 20, bh: code })
  return (
    result.records.find(
      (row) => String(row.bh ?? '').trim().toLowerCase() === code.trim().toLowerCase(),
    ) ?? null
  )
}

async function describeProject(code: string): Promise<string> {
  const project = await findProject(code)
  if (!project) {
    return `没有找到样品 ${code}，请确认编号是否正确。`
  }
  lastProjectCode = code
  return [
    `样品 ${code}：${text(project.productName) || '未命名'}`,
    `受检单位 ${text(project.dwname_sj_nj) || '未填写'}`,
    `登记日期 ${text(project.date_reg) || '未登记'}`,
    `结论 ${text(project.result) || '待定'}`,
  ].join('，') + '。需要我帮你打开页面或修改备注吗？'
}

async function updateRemark(code: string, value: string): Promise<string> {
  const project = await findProject(code)
  if (!project) {
    return `没有找到样品 ${code}。`
  }
  lastProjectCode = code
  try {
    await ElMessageBox.confirm(
      `确认把样品 ${code} 的登记备注改为「${value}」吗？`,
      '修改数据',
      { type: 'warning', confirmButtonText: '确认修改', cancelButtonText: '取消' },
    )
  } catch {
    return '好的，已取消修改。'
  }
  await updateData('project', {
    id_nj: project.id_nj,
    remarks_register: value,
  })
  return `已把 ${code} 的登记备注改为「${value}」。`
}

async function removeProject(code: string): Promise<string> {
  const project = await findProject(code)
  if (!project) {
    return `没有找到样品 ${code}。`
  }
  lastProjectCode = code
  try {
    await ElMessageBox.confirm(
      `删除后不可恢复，确认删除样品 ${code}（${text(project.productName) || '未命名'}）吗？`,
      '删除数据',
      {
        type: 'error',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return '好的，已取消删除。'
  }
  await deleteData('project', { id_nj: String(project.id_nj ?? '') })
  return `样品 ${code} 已删除。`
}

function chatReply(message: string): string {
  if (/天气/.test(message)) {
    return '我暂时查不了天气，不过可以帮你看看今天还有多少待办项目。'
  }
  if (/(累|忙|烦|加班)/.test(message)) {
    return '辛苦了，先把待办清一清吧。需要我帮你统计还有多少待安排和待检验吗？'
  }
  if (/(谢谢|再见|拜拜)/.test(message)) {
    return '好的，需要我时点一下右下角的浮球就行。'
  }
  if (message.length <= 4) {
    if (lastProjectCode) {
      return `嗯，我在。你刚才在查 ${lastProjectCode}，需要我打开它的登记信息吗？可以说“打开页面”。`
    }
    return '嗯，我在。你可以说“打开数据录入”或“查样品 JK(2023)-SA-182”。'
  }
  return '这个问题我暂时给不出专业回答。我目前最擅长：打开页面、查询样品、统计待办、修改备注和删除数据。你可以问我“你能做什么”。'
}

function text(value: unknown): string {
  return value === null || value === undefined ? '' : String(value).trim()
}

onMounted(() => {
  const stored = localStorage.getItem('lims_ai_pos')
  if (stored) {
    try {
      const saved = JSON.parse(stored) as { x?: number; y?: number }
      pos.x = Number(saved.x ?? 0)
      pos.y = Number(saved.y ?? 0)
    } catch {
      pos.x = 0
      pos.y = 0
    }
  }
  if (!stored || (!pos.x && !pos.y)) {
    pos.x = window.innerWidth - BALL_SIZE - 28
    pos.y = window.innerHeight - BALL_SIZE - 120
  }
  clampPosition()
  window.addEventListener('resize', clampPosition)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', clampPosition)
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  recognition?.stop()
  window.speechSynthesis?.cancel()
})
</script>

<style scoped>
.ai-assistant {
  position: relative;
  z-index: 3000;
}

.ai-ball {
  position: fixed;
  z-index: 3001;
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  font-size: 26px;
  cursor: grab;
  user-select: none;
  touch-action: none;
  background: linear-gradient(135deg, #4f95cf, #2f6fb3);
  box-shadow: 0 10px 26px rgba(47, 111, 179, 0.45);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ai-ball:hover {
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 14px 32px rgba(47, 111, 179, 0.55);
}

.ai-ball.dragging {
  cursor: grabbing;
  transform: scale(1.02);
  transition: none;
}

.ai-ball.open {
  background: linear-gradient(135deg, #2f6fb3, #1f4a6b);
}

.ai-ball-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 2px solid rgba(79, 149, 207, 0.45);
  animation: ai-pulse 2.4s ease-out infinite;
  pointer-events: none;
}

@keyframes ai-pulse {
  0% {
    transform: scale(0.92);
    opacity: 0.8;
  }

  70% {
    transform: scale(1.18);
    opacity: 0;
  }

  100% {
    opacity: 0;
  }
}

.ai-panel {
  position: fixed;
  z-index: 3002;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e3ebf4;
  border-radius: 16px;
  box-shadow: 0 18px 50px rgba(15, 40, 70, 0.22);
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #1f4a6b, #2f6fb3);
}

.ai-avatar {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 18px;
}

.ai-head-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.ai-head-text strong {
  font-size: 14px;
}

.ai-head-text small {
  color: rgba(255, 255, 255, 0.75);
  font-size: 11px;
}

.ai-icon-btn {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  padding: 0;
  color: rgba(255, 255, 255, 0.85);
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.ai-icon-btn:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.16);
}

.ai-messages {
  flex: 1;
  padding: 14px;
  overflow-y: auto;
  background: #f6f9fc;
}

.ai-msg {
  display: flex;
  margin-bottom: 10px;
}

.ai-msg.user {
  justify-content: flex-end;
}

.ai-bubble {
  max-width: 82%;
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-msg.assistant .ai-bubble {
  color: #2b3a4d;
  background: #ffffff;
  border: 1px solid #e6eef7;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(23, 50, 77, 0.05);
}

.ai-msg.user .ai-bubble {
  color: #ffffff;
  background: linear-gradient(135deg, #4f95cf, #2f6fb3);
  border-bottom-right-radius: 4px;
}

.ai-typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 12px 14px;
}

.ai-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9db4cc;
  animation: ai-blink 1.2s infinite ease-in-out;
}

.ai-typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.ai-typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes ai-blink {
  0%,
  80%,
  100% {
    opacity: 0.35;
    transform: translateY(0);
  }

  40% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.ai-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 14px 10px;
  background: #f6f9fc;
}

.ai-chips button {
  padding: 5px 10px;
  color: #2f6fb3;
  font-size: 12px;
  background: #ffffff;
  border: 1px solid #d8e6f4;
  border-radius: 999px;
  cursor: pointer;
}

.ai-chips button:hover {
  background: #eef5fc;
}

.ai-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #ffffff;
  border-top: 1px solid #edf2f8;
}

.ai-input input {
  flex: 1;
  min-width: 0;
  height: 36px;
  padding: 0 12px;
  color: #2b3a4d;
  font-size: 13px;
  background: #f5f8fc;
  border: 1px solid #e3ebf4;
  border-radius: 999px;
  outline: none;
}

.ai-input input:focus {
  border-color: #7fb0da;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(79, 149, 207, 0.14);
}

.ai-mic,
.ai-send {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  padding: 0;
  color: #ffffff;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
}

.ai-mic {
  background: #8aa4bd;
}

.ai-mic.recording {
  background: #d9534f;
  animation: ai-rec 1.2s infinite;
}

@keyframes ai-rec {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(217, 83, 79, 0.45);
  }

  50% {
    box-shadow: 0 0 0 8px rgba(217, 83, 79, 0);
  }
}

.ai-send {
  background: linear-gradient(135deg, #4f95cf, #2f6fb3);
}

.ai-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.ai-pop-enter-active,
.ai-pop-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.ai-pop-enter-from,
.ai-pop-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

@media (max-width: 480px) {
  .ai-panel {
    width: calc(100vw - 24px) !important;
    height: 70vh !important;
    left: 12px !important;
    top: auto !important;
    bottom: 12px !important;
  }
}
</style>
