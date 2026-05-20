import request from '@/utils/request'

/** 上传文件 */
export function uploadFileApi(file: File, category: string = 'AVATAR'): Promise<{ url: string }> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  return request.post('/api/v1/common/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
