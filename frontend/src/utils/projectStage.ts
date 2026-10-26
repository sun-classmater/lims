/**
 * project 表各业务环节信息列的统一判定
 *
 * 状态位含义（库内实测）：
 * - T：该环节已完成
 * - R：该环节处理中 / 已接收（已产生环节信息）
 * - F：未开始
 *
 * 登记信息、安排信息、检验报告：仅完成态(T)显示链接文字；
 * 分解信息、检验数据：完成(T)或处理中(R)均显示（旧 DEMO 中 SA-523
 * 即 state_div=R 仍显示「分解信息」）；
 * 报告递送信息：以 sentreport 字段是否有记录为准。
 */
export type ProjectStage =
  | 'register'
  | 'divide'
  | 'arrange'
  | 'report'
  | 'examine'
  | 'delivery'

export type ProjectStageRow = Record<string, unknown>

export function isStageInfoVisible(
  row: ProjectStageRow,
  stage: ProjectStage,
): boolean {
  switch (stage) {
    case 'register':
      return row.state_register === 'T'
    case 'divide':
      return row.state_div === 'T' || row.state_div === 'R'
    case 'arrange':
      return row.state_arrange === 'T'
    case 'report':
      return row.state_reportprint === 'T'
    case 'examine':
      return row.state_examine === 'T' || row.state_examine === 'R'
    case 'delivery': {
      const sent = row.sentreport
      return sent !== null && sent !== undefined && String(sent).trim() !== ''
    }
  }
}

export const projectStageLabels: Record<ProjectStage, string> = {
  register: '登记信息',
  divide: '分解信息',
  arrange: '安排信息',
  report: '检验报告',
  examine: '检验数据',
  delivery: '报告递送信息',
}
