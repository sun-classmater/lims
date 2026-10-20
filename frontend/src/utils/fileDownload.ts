import type { AxiosResponse } from 'axios'

/**
 * 保存后端返回的文件流。
 *
 * - 响应体其实是 JSON 时，说明后端按统一响应返回了业务错误，抛出其中的 msg；
 * - 文件名优先取响应头 filename*（含中文），取不到时用调用方给的默认名。
 */
export async function saveBlobResponse(
  response: AxiosResponse<Blob>,
  fallbackName: string,
): Promise<void> {
  const blob = response.data
  if (blob.type.includes('json')) {
    throw new Error(await readErrorMessage(blob))
  }

  const disposition = String(response.headers['content-disposition'] ?? '')
  saveBlob(blob, resolveFileName(disposition, fallbackName))
}

async function readErrorMessage(blob: Blob): Promise<string> {
  try {
    const body = JSON.parse(await blob.text()) as { msg?: string }
    return body.msg || '文件生成失败'
  } catch {
    return '文件生成失败'
  }
}

function resolveFileName(disposition: string, fallbackName: string): string {
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1])
    } catch {
      // 解码失败时退回默认文件名
    }
  }
  const plainMatch = /filename="?([^";]+)"?/i.exec(disposition)
  if (plainMatch) {
    return plainMatch[1]
  }
  return fallbackName
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
