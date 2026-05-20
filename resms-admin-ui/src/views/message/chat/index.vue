<template>
  <div class="chat-container p-4 h-[calc(100vh-100px)] flex gap-4 bg-[#f1f3f4]">
    <!-- 会话列表 -->
    <div class="w-[340px] bg-white rounded-[24px] flex flex-col overflow-hidden shadow-sm border border-gray-100">
      <div class="p-4 border-b border-gray-50">
        <h2 class="text-base font-bold text-[#1f1f1f] mb-3">消息</h2>
        <el-input v-model="searchText" placeholder="搜索联系人..." :prefix-icon="Search" class="chat-search-input"
          clearable />
      </div>
      <div class="flex-1 overflow-y-auto">
        <!-- 会话加载中 -->
        <div v-if="sessionsLoading" class="flex items-center justify-center py-16">
          <el-icon class="is-loading text-[#1a73e8]" :size="24">
            <Loading />
          </el-icon>
        </div>
        <!-- 无会话 -->
        <div v-else-if="filteredSessions.length === 0"
          class="flex flex-col items-center justify-center py-16 text-gray-400">
          <el-icon :size="48">
            <ChatDotSquare />
          </el-icon>
          <p class="mt-3 text-sm">暂无会话</p>
        </div>
        <!-- 会话列表 -->
        <div v-else>
          <div v-for="s in filteredSessions" :key="s.id"
            class="flex items-center gap-3 px-4 py-3.5 cursor-pointer transition-colors hover:bg-gray-50 border-b border-gray-50/50 group"
            :class="activeSession?.id === s.id ? 'bg-blue-50/70' : ''" @click="selectSession(s)">
            <el-badge :is-dot="s.unreadCount > 0" :value="s.unreadCount > 99 ? '99+' : s.unreadCount"
              :hidden="s.unreadCount === 0" class="chat-badge">
              <el-avatar :size="44" :src="sessionAvatarUrl(s) || undefined"
                class="!bg-blue-100 !text-blue-600 font-bold flex-shrink-0">
                {{ sessionAvatarText(s) }}
              </el-avatar>
            </el-badge>
            <div class="flex-1 min-w-0">
              <div class="flex justify-between items-center">
                <span class="text-sm font-semibold text-[#1f1f1f] truncate">{{ sessionName(s) }}</span>
                <span class="text-[10px] text-gray-400 whitespace-nowrap ml-2">{{ formatTime(s.lastMessageTime)
                }}</span>
              </div>
              <p class="text-xs text-gray-500 truncate mt-0.5">{{ s.lastMessageContent || '' }}</p>
            </div>
            <el-button
              class="!p-0 !w-7 !h-7 !rounded-full shrink-0 opacity-0 group-hover:opacity-100 transition-opacity !text-gray-400 hover:!text-red-500 hover:!bg-red-50"
              text size="small" @click.stop="handleDeleteSession(s)">
              <el-icon>
                <Delete />
              </el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 聊天主区域 -->
    <div class="flex-1 bg-white rounded-[24px] flex flex-col overflow-hidden shadow-sm border border-gray-100 relative">
      <!-- 未选择会话 -->
      <template v-if="!activeSession">
        <div class="flex-1 flex flex-col items-center justify-center text-gray-400">
          <el-icon :size="72">
            <ChatLineSquare />
          </el-icon>
          <p class="mt-4 text-base">选择一个会话开始聊天</p>
        </div>
      </template>

      <template v-else>
        <!-- 会话头部 -->
        <div class="px-6 py-4 border-b border-gray-50 flex items-center justify-between bg-white shrink-0">
          <div class="flex items-center gap-3">
            <el-avatar :size="40" :src="sessionAvatarUrl(activeSession) || undefined"
              class="!bg-blue-100 !text-blue-600 font-bold flex-shrink-0">
              {{ sessionAvatarText(activeSession) }}
            </el-avatar>
            <div>
              <h3 class="font-bold text-sm text-[#1f1f1f]">{{ sessionName(activeSession) }}</h3>
              <span class="text-[10px] text-green-500">在线</span>
            </div>
          </div>
          <el-button text icon="Refresh" @click="refreshMessages">刷新</el-button>
        </div>

        <!-- 消息区域 -->
        <div ref="messageContainer" class="flex-1 overflow-y-auto px-6 py-4 space-y-1 bg-[#f8f9fa]"
          @scroll="onMessageScroll">
          <!-- 消息加载中 -->
          <div v-if="messagesLoading" class="flex justify-center py-4">
            <el-icon class="is-loading text-[#1a73e8]" :size="20">
              <Loading />
            </el-icon>
          </div>
          <!-- 无消息 -->
          <div v-else-if="messages.length === 0" class="flex flex-col items-center justify-center py-16 text-gray-400">
            <el-icon :size="40">
              <ChatDotSquare />
            </el-icon>
            <p class="mt-2 text-sm">暂无消息，发送第一条消息吧</p>
          </div>
          <!-- 消息列表 -->
          <template v-else>
            <div v-for="(msg, idx) in messages" :key="msg.id">
              <!-- 日期分隔 -->
              <div v-if="showDateSeparator(idx)" class="flex justify-center py-2">
                <span class="text-[10px] text-gray-400 bg-gray-100 px-3 py-1 rounded-full">{{ formatDate(msg.createTime)
                }}</span>
              </div>
              <!-- 系统消息 -->
              <div v-if="msg.msgType === 4" class="flex justify-center py-1">
                <span class="text-[10px] text-gray-400 bg-gray-100 px-3 py-1 rounded-full">{{ msg.content }}</span>
              </div>
              <!-- 已撤回消息 -->
              <div v-else-if="msg.isRecalled === 1" class="flex justify-center py-1">
                <span class="text-[10px] text-gray-400 italic">
                  你撤回了一条消息
                </span>
              </div>
              <!-- 普通消息 -->
              <div v-else
                :class="['flex items-end gap-2', (msg.senderId === currentUserId && msg.senderType === 1) ? 'justify-end' : 'justify-start']">
                <!-- 他人头像（左侧） -->
                <el-avatar v-if="!(msg.senderId === currentUserId && msg.senderType === 1)" :size="32" :src="msgAvatarUrl(msg) || undefined"
                  class="!bg-blue-100 !text-blue-600 text-xs font-bold flex-shrink-0 mb-1">
                  {{ (msg.senderName || '?').charAt(0).toUpperCase() }}
                </el-avatar>

                <div class="max-w-[60%] min-w-0">
                  <!-- 发送者昵称（他人消息始终显示） -->
                  <div v-if="!(msg.senderId === currentUserId && msg.senderType === 1)" class="text-[11px] text-gray-400 px-1 mb-1 font-medium">
                    {{ getSenderName(msg) }}
                  </div>
                  <!-- 文本消息 -->
                  <!-- 文本消息 -->
                  <div v-if="msg.msgType === 1">
                    <!-- 房源小卡片 -->
                    <div v-if="parseHouseCard(msg.content)"
                      class="p-4 rounded-2xl bg-white border border-gray-150 text-gray-800 shadow-sm flex flex-col gap-3 min-w-[240px] max-w-[280px]">
                      <div class="text-[11px] font-bold text-gray-400 flex items-center gap-1 select-none">
                        <span>🏠 咨询房源</span>
                      </div>
                      <img 
                        v-if="parseHouseCard(msg.content)?.coverUrl" 
                        :src="parseHouseCard(msg.content)!.coverUrl!" 
                        class="w-full h-32 object-cover rounded-xl shadow-xs" 
                        alt="房源图片" 
                      />
                      <div class="flex flex-col gap-1 text-left">
                        <div class="font-extrabold text-[14px] text-gray-900 truncate">{{ parseHouseCard(msg.content)!.projectName }}</div>
                        <div class="text-[12px] text-gray-500 flex items-center gap-1.5 mt-0.5">
                          <span class="font-medium text-gray-400">售价/租金：</span>
                          <span class="font-black text-red-650">{{ parseHouseCard(msg.content)!.price }}</span>
                        </div>
                        <div class="text-[12px] text-gray-500 flex items-center gap-1.5">
                          <span class="font-medium text-gray-400">户型/面积：</span>
                          <span class="font-semibold text-gray-700">{{ parseHouseCard(msg.content)!.layout }}</span>
                        </div>
                      </div>
                      <button 
                        v-if="parseHouseCard(msg.content)?.houseId"
                        type="button"
                        class="mt-1 text-center text-xs font-black text-indigo-600 hover:text-indigo-700 bg-indigo-50 hover:bg-indigo-100 py-2.5 rounded-xl border border-indigo-100 transition-all flex items-center justify-center cursor-pointer select-none w-full"
                        @click="showHouseDetail(parseHouseCard(msg.content)!.houseId!)"
                      >
                        点击查看房源详情
                      </button>
                      
                      <!-- 底部状态与时间 -->
                      <div class="flex items-center justify-end gap-2 mt-1 pt-1.5 border-t border-gray-100">
                        <span v-if="canRecall(msg)" class="text-[10px] cursor-pointer hover:underline text-gray-450"
                          @click="handleRecall(msg)">撤回</span>
                        <span class="text-[10px] text-gray-400">
                          {{ formatTime(msg.createTime, true) }}
                        </span>
                      </div>
                    </div>

                    <!-- 普通文本消息 -->
                    <div v-else
                      class="px-4 py-2.5 rounded-2xl text-sm leading-relaxed shadow-sm break-words"
                      :class="(msg.senderId === currentUserId && msg.senderType === 1) ? 'bg-[#1a73e8] text-white rounded-br-none' : 'bg-white text-gray-800 rounded-bl-none border border-gray-100'">
                      {{ msg.content }}
                      <div class="flex items-center justify-end gap-2 mt-0.5">
                        <span v-if="canRecall(msg)" class="text-[10px] cursor-pointer hover:underline"
                          :class="(msg.senderId === currentUserId && msg.senderType === 1) ? 'text-blue-200' : 'text-gray-400'"
                          @click="handleRecall(msg)">撤回</span>
                        <span class="text-[10px]"
                          :class="(msg.senderId === currentUserId && msg.senderType === 1) ? 'text-blue-200' : 'text-gray-400'">
                          {{ formatTime(msg.createTime, true) }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <!-- 图片消息 -->
                  <div v-else-if="msg.msgType === 2" class="space-y-1">
                    <div v-if="msg.fileUrl"
                      class="rounded-2xl overflow-hidden shadow-sm border border-gray-100 max-w-[280px] cursor-pointer"
                      @click="previewImage(msg.fileUrl!)">
                      <el-image :src="fileUrl(msg)" fit="cover" class="w-full max-h-64" loading="lazy" />
                    </div>
                    <div class="flex items-center justify-end gap-2">
                      <span v-if="canRecall(msg)" class="text-[10px] text-gray-400 cursor-pointer hover:underline"
                        @click="handleRecall(msg)">撤回</span>
                      <span class="text-[10px] text-gray-400">{{ formatTime(msg.createTime, true) }}</span>
                    </div>
                  </div>
                  <!-- 文件消息 -->
                  <div v-else-if="msg.msgType === 3" class="space-y-1">
                    <div
                      class="px-4 py-3 rounded-2xl text-sm shadow-sm flex items-center gap-3 min-w-[200px] cursor-pointer hover:opacity-80"
                      :class="(msg.senderId === currentUserId && msg.senderType === 1) ? 'bg-[#1a73e8] text-white rounded-br-none' : 'bg-white text-gray-800 rounded-bl-none border border-gray-100'"
                      @click="downloadFile(msg)">
                      <el-icon :size="28" class="shrink-0">
                        <Document />
                      </el-icon>
                      <div class="min-w-0 flex-1">
                        <p class="text-sm truncate font-medium">{{ msg.fileName || '未知文件' }}</p>
                        <p class="text-[10px] mt-0.5 opacity-70">{{ formatFileSize(msg.fileSize) }}</p>
                      </div>
                      <el-icon class="shrink-0">
                        <Download />
                      </el-icon>
                    </div>
                    <div class="flex items-center justify-end gap-2">
                      <span v-if="canRecall(msg)" class="text-[10px] text-gray-400 cursor-pointer hover:underline"
                        @click="handleRecall(msg)">撤回</span>
                      <span class="text-[10px] text-gray-400">{{ formatTime(msg.createTime, true) }}</span>
                    </div>
                  </div>
                </div>

                <!-- 自己头像（右侧） -->
                <el-avatar v-if="msg.senderId === currentUserId && msg.senderType === 1" :size="32" :src="currentUserAvatarUrl || undefined"
                  class="!bg-[#1a73e8] !text-white text-xs font-bold flex-shrink-0 mb-1">
                  {{ (userStore.userInfo?.nickName || userStore.userInfo?.username || '我').charAt(0).toUpperCase() }}
                </el-avatar>
              </div>
            </div>
          </template>
          <!-- 底部锚点 -->
          <div ref="scrollAnchor" />
        </div>

        <!-- 图片预览 Dialog -->
        <el-dialog v-model="previewVisible" :show-close="true" width="auto" align-center class="image-preview-dialog">
          <img :src="previewUrl" class="max-w-[80vw] max-h-[80vh] object-contain rounded-lg" alt="图片预览" />
        </el-dialog>

        <!-- 输入区域 -->
        <div class="px-6 py-3 border-t border-gray-50 bg-white shrink-0">
          <!-- 上传预览 -->
          <div v-if="uploadPreview" class="flex items-center gap-3 mb-3 px-3 py-2 bg-gray-50 rounded-xl">
            <el-icon v-if="uploadPreview.type === 'image'" class="text-green-500" :size="20">
              <Picture />
            </el-icon>
            <el-icon v-else class="text-blue-500" :size="20">
              <Folder />
            </el-icon>
            <span class="text-sm text-gray-600 truncate flex-1">{{ uploadPreview.name }}</span>
            <el-button text icon="Close" size="small" @click="clearUpload" />
          </div>
          <div class="flex items-end gap-2">
            <div class="flex items-center gap-1 pb-1">
              <el-upload :show-file-list="false" :before-upload="beforeImageUpload" :http-request="uploadFile"
                accept="image/*" action="">
                <el-button text class="chat-toolbar-btn" title="发送图片">
                  <el-icon :size="22">
                    <Picture />
                  </el-icon>
                </el-button>
              </el-upload>
              <el-upload :show-file-list="false" :before-upload="beforeFileUpload" :http-request="uploadFile" accept="*"
                action="">
                <el-button text class="chat-toolbar-btn" title="发送文件">
                  <el-icon :size="22">
                    <Folder />
                  </el-icon>
                </el-button>
              </el-upload>
            </div>
            <el-input v-model="inputMsg" type="textarea" :rows="2" placeholder="输入消息..." resize="none"
              class="chat-input" @keyup.enter.prevent="handleSendText" />
            <el-button type="primary" :icon="Promotion"
              class="!rounded-xl !bg-[#1a73e8] border-none !h-[52px] !w-[52px] shrink-0" :loading="sending"
              @click="handleSendText" />
          </div>
        </div>
      </template>
    </div>

    <!-- 房源详情抽屉 -->
    <el-drawer
      v-model="houseDrawerVisible"
      title="房源详情"
      direction="rtl"
      size="500px"
      destroy-on-close
      class="house-detail-drawer"
    >
      <div v-if="houseLoading" class="flex justify-center py-12">
        <el-icon class="is-loading text-[#1a73e8]" :size="32">
          <Loading />
        </el-icon>
      </div>
      <div v-else-if="!houseDetail" class="flex flex-col items-center justify-center py-12 text-gray-400">
        <el-icon :size="48"><Warning /></el-icon>
        <p class="mt-2 text-sm">房源详情加载失败或房源不存在</p>
      </div>
      <div v-else class="space-y-6 overflow-y-auto max-h-[calc(100vh-100px)] pr-2">
        <!-- 房源多图轮播与单图展示 -->
        <div v-if="houseDetail.images && houseDetail.images.length > 1">
          <el-carousel trigger="click" height="240px" class="rounded-2xl overflow-hidden shadow-xs">
            <el-carousel-item v-for="img in houseDetail.images" :key="img.id">
              <img :src="resolveUrl(img.url || img.fileKey)" class="w-full h-full object-cover" alt="房源图片" />
            </el-carousel-item>
          </el-carousel>
        </div>
        <div v-else-if="houseDetail.images && houseDetail.images.length === 1">
          <img :src="resolveUrl(houseDetail.images[0].url || houseDetail.images[0].fileKey)" class="w-full h-[240px] object-cover rounded-2xl shadow-xs" alt="房源图片" />
        </div>
        <div v-else class="w-full h-[240px] bg-gray-100 flex flex-col items-center justify-center text-gray-300 rounded-2xl shadow-xs border border-gray-150">
          <el-icon :size="48"><Picture /></el-icon>
          <span class="text-xs mt-2 font-bold">暂无房源图片</span>
        </div>

        <!-- 基本信息 -->
        <div class="bg-gray-50 p-5 rounded-2xl space-y-4">
          <div class="flex justify-between items-start gap-4">
            <div class="min-w-0">
              <h2 class="text-lg font-black text-gray-900 truncate">{{ houseDetail.house?.projectName }}</h2>
              <p class="text-[11px] text-gray-450 mt-1">房源编号：{{ houseDetail.house?.houseNo || '暂无' }}</p>
            </div>
            <div class="text-right shrink-0">
              <div class="text-xl font-black text-red-600">
                {{ houseDetail.price }}
                <span class="text-xs font-semibold text-gray-500">
                  {{ houseDetail.priceUnit === 1 ? '万' : '元/月' }}
                </span>
              </div>
              <p v-if="houseDetail.unitPrice" class="text-[10px] text-gray-400 mt-0.5">单价：{{ houseDetail.unitPrice }}元/㎡</p>
            </div>
          </div>

          <!-- 标签 -->
          <div v-if="houseDetail.house?.tags" class="flex flex-wrap gap-1.5">
            <el-tag 
              v-for="tag in (Array.isArray(houseDetail.house.tags) ? houseDetail.house.tags : JSON.parse(houseDetail.house.tags || '[]'))" 
              :key="tag" 
              size="small" 
              effect="plain"
              type="info"
              class="!rounded-md"
            >
              {{ tag }}
            </el-tag>
          </div>

          <!-- 信息格网 -->
          <div class="grid grid-cols-2 gap-y-3 gap-x-4 border-t border-gray-100 pt-4 text-xs text-gray-700">
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">房源类型：</span><span class="font-bold">{{ getHouseTypeText(houseDetail.house?.houseType) }}</span></div>
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">户型：</span><span class="font-bold">{{ houseDetail.house?.layout }}</span></div>
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">建筑面积：</span><span class="font-bold">{{ houseDetail.house?.area }} ㎡</span></div>
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">朝向：</span><span class="font-bold">{{ houseDetail.house?.orientation }}</span></div>
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">楼层：</span><span class="font-bold">{{ houseDetail.house?.floor }} / {{ houseDetail.house?.totalFloor }} 层</span></div>
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">装修：</span><span class="font-bold">{{ houseDetail.house?.decoration }}</span></div>
            <div class="flex items-center col-span-2"><span class="text-gray-400 w-16 shrink-0">具体位置：</span><span class="font-bold truncate">{{ houseDetail.house?.buildingNo }}栋{{ houseDetail.house?.unitNo }}单元{{ houseDetail.house?.roomNo }}室</span></div>
            <div class="flex items-center"><span class="text-gray-400 w-16 shrink-0">状态：</span>
              <el-tag :type="getStatusTagType(houseDetail.house?.status)" size="small" class="!rounded-md">
                {{ getStatusText(houseDetail.house?.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 房源描述 -->
        <div class="space-y-2">
          <h3 class="text-xs font-bold text-gray-900 flex items-center gap-1.5">
            <span class="w-1.5 h-3.5 bg-indigo-600 rounded-sm"></span>
            房源描述
          </h3>
          <p class="text-xs text-gray-650 leading-relaxed bg-gray-50 p-4 rounded-2xl whitespace-pre-line">
            {{ houseDetail.house?.description || '暂无详细描述。' }}
          </p>
        </div>

        <!-- 扩展信息 -->
        <div v-if="hasExtendInfo(houseDetail)" class="space-y-2">
          <h3 class="text-xs font-bold text-gray-900 flex items-center gap-1.5">
            <span class="w-1.5 h-3.5 bg-indigo-600 rounded-sm"></span>
            扩展信息
          </h3>
          <div class="bg-gray-50 p-4 rounded-2xl text-xs text-gray-700 space-y-3">
            <!-- 新房扩展 -->
            <template v-if="houseDetail.house?.houseType === 1 && houseDetail.newHouseExtend">
              <div class="grid grid-cols-2 gap-3">
                <div><span class="text-gray-400">产权年限：</span><span class="font-bold">{{ houseDetail.newHouseExtend.propertyRights }}年</span></div>
                <div><span class="text-gray-400">绿化率：</span><span class="font-bold">{{ houseDetail.newHouseExtend.greenRate }}%</span></div>
                <div><span class="text-gray-400">容积率：</span><span class="font-bold">{{ houseDetail.newHouseExtend.plotRate }}</span></div>
                <div><span class="text-gray-400">开盘时间：</span><span class="font-bold">{{ formatDateString(houseDetail.newHouseExtend.openTime) }}</span></div>
                <div class="col-span-2"><span class="text-gray-400">开发商：</span><span class="font-bold">{{ houseDetail.newHouseExtend.developer }}</span></div>
              </div>
            </template>
            <!-- 二手房扩展 -->
            <template v-if="houseDetail.house?.houseType === 2 && houseDetail.secondHouseExtend">
              <div class="grid grid-cols-2 gap-3">
                <div><span class="text-gray-400">建筑年代：</span><span class="font-bold">{{ houseDetail.secondHouseExtend.buildYear }}年</span></div>
                <div><span class="text-gray-400">产权类型：</span><span class="font-bold">{{ houseDetail.secondHouseExtend.propertyType }}</span></div>
                <div><span class="text-gray-400">是否有电梯：</span><span class="font-bold">{{ houseDetail.secondHouseExtend.hasElevator === 1 ? '是' : '否' }}</span></div>
                <div><span class="text-gray-400">唯一住房：</span><span class="font-bold">{{ houseDetail.secondHouseExtend.isUnique === 1 ? '是' : '否' }}</span></div>
                <div class="col-span-2"><span class="text-gray-400">产权性质：</span><span class="font-bold">{{ houseDetail.secondHouseExtend.propertyRights || '商品房' }}</span></div>
              </div>
            </template>
            <!-- 租房扩展 -->
            <template v-if="houseDetail.house?.houseType === 3 && houseDetail.rentHouseExtend">
              <div class="grid grid-cols-2 gap-3">
                <div><span class="text-gray-400">租赁方式：</span><span class="font-bold">{{ houseDetail.rentHouseExtend.rentType === 1 ? '整租' : '合租' }}</span></div>
                <div><span class="text-gray-400">付款方式：</span><span class="font-bold">{{ houseDetail.rentHouseExtend.payType || '押一付三' }}</span></div>
                <div><span class="text-gray-400">看房时间：</span><span class="font-bold">{{ houseDetail.rentHouseExtend.checkTime || '随时看房' }}</span></div>
                <div><span class="text-gray-400">入住时间：</span><span class="font-bold">{{ formatDateString(houseDetail.rentHouseExtend.checkInTime) }}</span></div>
              </div>
            </template>
          </div>
        </div>

        <!-- 负责销售 -->
        <div class="flex items-center gap-3 bg-indigo-50/50 border border-indigo-50/50 p-4 rounded-2xl">
          <el-avatar :size="40" :src="resolveUrl(houseDetail.salesAvatar)" class="!bg-indigo-100 !text-indigo-600 font-bold shrink-0">
            {{ (houseDetail.salesName || '销').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="min-w-0 flex-1">
            <h4 class="font-extrabold text-xs text-gray-900">专属顾问</h4>
            <p class="text-[10px] text-gray-500 mt-1">顾问姓名：{{ houseDetail.salesName || '置业顾问' }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Loading, ChatDotSquare, ChatLineSquare, Picture, Folder, Document, Download, Promotion, Refresh, Close, Delete, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import type { ChatSession, ChatMessage } from '@/api/message/chat'
import { listSessions, listMessages, sendMessage as apiSendMessage, readSession, recallMessage as apiRecallMessage, deleteSession as apiDeleteSession } from '@/api/message/chat'
import type { UploadProps } from 'element-plus'
import { wsService } from '@/utils/WebSocketService'
import { getHouse } from '@/api/house/house'

// ============================================================
// 状态
// ============================================================
const userStore = useUserStore()
const currentUserId = computed(() => Number(userStore.userInfo?.id ?? 0))
const currentUserAvatarUrl = computed(() => resolveUrl(userStore.userInfo?.avatar || ''))
const route = useRoute()

const searchText = ref('')
const sessions = ref<ChatSession[]>([])
const sessionsLoading = ref(false)
const activeSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const messagesLoading = ref(false)
const inputMsg = ref('')
const sending = ref(false)
const messageContainer = ref<HTMLElement | null>(null)
const scrollAnchor = ref<HTMLElement | null>(null)
const previewUrl = ref('')
const previewVisible = ref(false)
const uploadPreview = ref<{ file: File; type: 'image' | 'file'; name: string } | null>(null)
const allLoaded = ref(false)
const pageNum = ref(1)
const PAGE_SIZE = 30

// ============================================================
// 计算属性
// ============================================================
const filteredSessions = computed(() => {
  if (!searchText.value) return sessions.value
  const q = searchText.value.toLowerCase()
  return sessions.value.filter(s => sessionName(s).toLowerCase().includes(q))
})

// ============================================================
// 会话列表
// ============================================================
async function loadSessions() {
  sessionsLoading.value = true
  try {
    const { data } = await listSessions()
    sessions.value = data || []
  } catch {
    // request.ts 已处理错误提示
  } finally {
    sessionsLoading.value = false
  }
}

function sessionName(s: ChatSession): string {
  if (s.sessionName) return s.sessionName
  // 私聊显示对方名称
  const others = s.members?.filter(m => !(m.userType === 1 && m.userId === currentUserId.value)) || []
  return others.map(m => m.userName || `用户${m.userId}`).join(', ') || '未知会话'
}

/** 返回会话对方的头像 URL（私聊取对方成员头像，群聊暂返回空） */
function sessionAvatarUrl(s: ChatSession | null): string {
  if (!s) return ''
  const others = s.members?.filter(m => !(m.userType === 1 && m.userId === currentUserId.value)) || []
  return resolveUrl(others[0]?.userAvatar || '')
}

/**
 * 解析文件/图片 URL 用于前端展示
 * - http(s):// → 直接使用（外部资源）
 * - /api/profile/ → 直接使用（后端静态映射的公开资源）
 * - 其他相对路径 → 拼接 /api/profile/ 前缀（公开资源）
 */
function resolveUrl(url: string): string {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api/profile/')) return url
  return `/api/profile/${url.replace(/^\//, '')}`
}

/** 头像文字兜底（无图片时显示首字母） */
function sessionAvatarText(s: ChatSession | null): string {
  if (!s) return '?'
  const name = sessionName(s)
  return name.charAt(0).toUpperCase() || '?'
}

/** 消息发送者头像 URL */
function msgAvatarUrl(msg: ChatMessage): string {
  const member = activeSession.value?.members?.find(m => m.userId === msg.senderId && m.userType === msg.senderType)
  const url = member?.userAvatar || msg.senderAvatar || ''
  return resolveUrl(url)
}

/** 获取发送者名称（优先从当前会话成员列表中取，避免使用可能包含用户名的旧快照） */
function getSenderName(msg: ChatMessage): string {
  if (msg.senderId === currentUserId.value && msg.senderType === 1) {
    return userStore.userInfo?.nickName || userStore.userInfo?.realName || '我'
  }
  const member = activeSession.value?.members?.find(m => m.userId === msg.senderId && m.userType === msg.senderType)
  return member?.userName || msg.senderName || `用户${msg.senderId}`
}

// ============================================================
// 删除会话
// ============================================================
async function handleDeleteSession(s: ChatSession) {
  try {
    await ElMessageBox.confirm(
      `确定要删除与「${sessionName(s)}」的对话吗？删除后不可恢复。`,
      '删除对话',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await apiDeleteSession(s.id)
    ElMessage.success('对话已删除')
    // 如果删除的是当前活跃会话，清除选中状态
    if (activeSession.value?.id === s.id) {
      activeSession.value = null
      messages.value = []
    }
    await loadSessions()
  } catch {
    // 取消或失败都静默处理
  }
}

// ============================================================
// 选择会话
// ============================================================
async function selectSession(s: ChatSession) {
  if (activeSession.value?.id === s.id) return
  activeSession.value = s
  messages.value = []
  allLoaded.value = false
  pageNum.value = 1

  // 清零未读数
  if (s.unreadCount > 0) {
    try {
      await readSession(s.id)
      s.unreadCount = 0
    } catch { /* ignore */ }
  }

  await loadMessages(true)
}

// ============================================================
// 消息加载
// ============================================================
async function loadMessages(append = false) {
  if (!activeSession.value) return
  if (allLoaded.value && !append) return

  messagesLoading.value = true
  const targetPage = append ? pageNum.value : 1
  try {
    const { data } = await listMessages(activeSession.value.id, targetPage, PAGE_SIZE)
    if (data) {
      const list = data.records || []
      if (list.length < PAGE_SIZE) allLoaded.value = true

      if (append) {
        // 追加旧消息（翻页加载）
        messages.value = [...list.reverse(), ...messages.value]
      } else {
        messages.value = list.reverse()
        pageNum.value = 1
      }
    }
  } catch {
    // request.ts 已处理错误提示
  } finally {
    messagesLoading.value = false
  }

  if (!append) {
    await nextTick()
    scrollToBottom()
  }
}

async function refreshMessages() {
  pageNum.value = 1
  allLoaded.value = false
  await loadMessages(false)
}

// 滚到顶部加载更多
async function onMessageScroll(e: Event) {
  const el = e.target as HTMLElement
  if (el.scrollTop < 80 && !messagesLoading.value && !allLoaded.value) {
    pageNum.value++
    const prevScrollHeight = el.scrollHeight
    await loadMessages(true)
    await nextTick()
    el.scrollTop = el.scrollHeight - prevScrollHeight
  }
}

// 新消息轮询
let pollTimer: ReturnType<typeof setInterval> | null = null

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!activeSession.value) return
    try {
      const { data: freshSessions } = await listSessions()
      if (freshSessions) {
        sessions.value = freshSessions
      }
      // 仅当会话活跃时拉取新消息
      const { data } = await listMessages(activeSession.value.id, 1, PAGE_SIZE)
      if (data) {
        const freshList = (data.records || []).reverse()
        if (freshList.length > messages.value.length) {
          messages.value = freshList
          await nextTick()
          scrollToBottom()
        }
      }
    } catch { /* silent */ }
  }, 10000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// ============================================================
// 发送消息
// ============================================================
async function handleSendText() {
  if (!activeSession.value) return
  const text = inputMsg.value.trim()
  if (!text) return

  sending.value = true
  try {
    await apiSendMessage({
      sessionId: activeSession.value.id,
      msgType: 1,
      content: text
    })
    inputMsg.value = ''
    // 刷新消息
    pageNum.value = 1
    allLoaded.value = false
    await loadMessages(false)
  } catch {
    // request.ts 已处理
  } finally {
    sending.value = false
  }
}

// ============================================================
// 文件上传
// ============================================================
const UPLOAD_URL = '/v1/common/upload'

async function uploadFile(options: any) {
  const file = options.file as File
  if (!file) return

  sending.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('category', 'CHAT')

    const res = await request({
      url: UPLOAD_URL,
      method: 'post',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    const url: string = res.data?.url

    if (!activeSession.value) return

    if (file.type.startsWith('image/')) {
      await apiSendMessage({
        sessionId: activeSession.value.id,
        msgType: 2,
        content: '[图片]',
        fileUrl: url,
        fileName: file.name,
        fileSize: file.size
      })
    } else {
      await apiSendMessage({
        sessionId: activeSession.value.id,
        msgType: 3,
        content: file.name,
        fileUrl: url,
        fileName: file.name,
        fileSize: file.size
      })
    }

    clearUpload()
    pageNum.value = 1
    allLoaded.value = false
    await loadMessages(false)
  } catch {
    ElMessage.error('文件上传失败')
  } finally {
    sending.value = false
  }
}

function beforeImageUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请选择图片文件')
    return false
  }
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  uploadPreview.value = { file, type: 'image', name: file.name }
  return true // 允许上传，流转到自定义 http-request
}

function beforeFileUpload(file: File) {
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  uploadPreview.value = { file, type: 'file', name: file.name }
  return true // 允许上传，流转到自定义 http-request
}

function clearUpload() {
  uploadPreview.value = null
}

// ============================================================
// 撤回消息
// ============================================================
function canRecall(msg: ChatMessage): boolean {
  if (msg.senderId !== currentUserId.value || msg.senderType !== 1) return false
  if (msg.isRecalled === 1) return false
  const elapsed = Date.now() - new Date(msg.createTime).getTime()
  return elapsed < 120_000
}

async function handleRecall(msg: ChatMessage) {
  try {
    await apiRecallMessage(msg.id)
    msg.isRecalled = 1
    ElMessage.success('已撤回')
  } catch {
    ElMessage.error('撤回失败')
  }
}

// ============================================================
// 图片预览 & 文件下载
// ============================================================
function fileUrl(msg: ChatMessage): string {
  return resolveUrl(msg.fileUrl || '')
}

function previewImage(url: string) {
  previewUrl.value = resolveUrl(url)
  previewVisible.value = true
}

function downloadFile(msg: ChatMessage) {
  if (!msg.fileUrl) return
  const url = fileUrl(msg)
  const a = document.createElement('a')
  a.href = url
  a.download = msg.fileName || 'file'
  a.target = '_blank'
  a.click()
}

// ============================================================
// 时间/日期格式化
// ============================================================
function formatTime(time: string | undefined, showTime = false): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')

  if (!showTime) {
    // 会话列表显示：今天显示时间，昨天显示"昨天"，更早显示日期
    const isToday = d.toDateString() === now.toDateString()
    const isYesterday = new Date(now.getTime() - 86400000).toDateString() === d.toDateString()
    if (isToday) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
    if (isYesterday) return '昨天'
    return `${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
  }

  // 消息时间戳
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(time: string | undefined): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')

  const isToday = d.toDateString() === now.toDateString()
  const isYesterday = new Date(now.getTime() - 86400000).toDateString() === d.toDateString()

  if (isToday) return '今天'
  if (isYesterday) return '昨天'
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function showDateSeparator(idx: number): boolean {
  if (idx === 0) return true
  const prev = messages.value[idx - 1]
  const curr = messages.value[idx]
  if (!prev || !curr) return false
  const pd = new Date(prev.createTime).toDateString()
  const cd = new Date(curr.createTime).toDateString()
  return pd !== cd
}

function scrollToBottom() {
  scrollAnchor.value?.scrollIntoView({ behavior: 'auto' })
}

// ============================================================
// WebSocket 消息处理
// ============================================================
let unsubMessage: (() => void) | null = null

/** 处理 WS 推送的新消息 */
function handleWSMessage(msg: ChatMessage) {
  // 更新会话列表（最后消息内容、时间等）
  const idx = sessions.value.findIndex(s => s.id === msg.sessionId)
  if (idx !== -1) {
    sessions.value[idx].lastMessageContent = msg.content
    sessions.value[idx].lastMessageTime = msg.createTime
    if (!(msg.senderId === currentUserId.value && msg.senderType === 1)) {
      sessions.value[idx].unreadCount = (sessions.value[idx].unreadCount || 0) + 1
    }
    // 置顶到最前
    const s = sessions.value.splice(idx, 1)[0]
    sessions.value.unshift(s)
  }

  // 如果消息属于当前活跃会话，追加到消息列表
  if (activeSession.value?.id === msg.sessionId && !(msg.senderId === currentUserId.value && msg.senderType === 1)) {
    messages.value.push(msg)
    nextTick(() => scrollToBottom())
  }
}

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await loadSessions()

  // 从路由参数中自动选中会话（从部门成员页发起聊天）
  const sessionIdParam = route.query.sessionId
  if (sessionIdParam) {
    const targetId = Number(sessionIdParam)
    if (!isNaN(targetId)) {
      const targetSession = sessions.value.find(s => s.id === targetId)
      if (targetSession) {
        await nextTick()
        selectSession(targetSession)
      }
    }
  }

  // 注册 WS 消息处理器（连接由全局 layout 管理）
  unsubMessage = wsService.on('MESSAGE', handleWSMessage)

  startPolling()
})

onUnmounted(() => {
  stopPolling()
  unsubMessage?.()
})

// 监听活跃会话变化，重新开始轮询
watch(activeSession, () => {
  startPolling()
})

function parseHouseCard(content: string | null | undefined) {
  if (!content) return null
  if (!content.includes('我正在看这套房源') && !content.includes('我正在咨询该房源')) return null
  
  const imgMatch = content.match(/!\[.*?\]\((.*?)\)/)
  const coverUrl = imgMatch && imgMatch[1] ? imgMatch[1] : null
  
  const nameMatch = content.match(/\*\*房源名称\*\*：(.*?)(?:\n|$)/)
  const projectName = nameMatch && nameMatch[1] ? nameMatch[1].trim() : ''
  
  const priceMatch = content.match(/\*\*售价\/租金\*\*：(.*?)(?:\n|$)/)
  const price = priceMatch && priceMatch[1] ? priceMatch[1].trim() : ''
  
  const layoutMatch = content.match(/\*\*户型\/面积\*\*：(.*?)(?:\n|$)/)
  const layout = layoutMatch && layoutMatch[1] ? layoutMatch[1].trim() : ''
  
  const idMatch = content.match(/\/house\/(\d+)/)
  const houseId = idMatch && idMatch[1] ? Number(idMatch[1]) : null
  
  if (!projectName && !price) return null
  
  return {
    coverUrl,
    projectName,
    price,
    layout,
    houseId
  }
}

function getHousePortalUrl(houseId: number) {
  const origin = window.location.origin
  if (origin.includes('localhost:5173')) {
    return `http://localhost:5174/house/${houseId}`
  } else if (origin.includes('127.0.0.1:5173')) {
    return `http://127.0.0.1:5174/house/${houseId}`
  }
  return origin.replace(':5173', ':5174') + `/house/${houseId}`
}

// 房源详情抽屉相关状态与方法
const houseDrawerVisible = ref(false)
const houseLoading = ref(false)
const houseDetail = ref<any>(null)

async function showHouseDetail(houseId: number) {
  houseDrawerVisible.value = true
  houseLoading.value = true
  houseDetail.value = null
  try {
    const { data } = await getHouse(houseId)
    houseDetail.value = data
  } catch (error) {
    ElMessage.error('获取房源详情失败')
  } finally {
    houseLoading.value = false
  }
}

function getHouseTypeText(type?: number) {
  if (type === 1) return '新房'
  if (type === 2) return '二手房'
  if (type === 3) return '租房'
  return '未知'
}

function getStatusText(status?: number) {
  if (status === 1) return '上架'
  if (status === 2) return '下架'
  if (status === 3) return '草稿'
  if (status === 4) return '待审核'
  if (status === 5) return '审核驳回'
  return '未知'
}

function getStatusTagType(status?: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 3) return 'info'
  if (status === 4) return 'warning'
  if (status === 5) return 'danger'
  return 'info'
}

function hasExtendInfo(detail: any) {
  if (!detail || !detail.house) return false
  const t = detail.house.houseType
  return (t === 1 && detail.newHouseExtend) || 
         (t === 2 && detail.secondHouseExtend) || 
         (t === 3 && detail.rentHouseExtend)
}

function formatDateString(dateStr?: string) {
  if (!dateStr) return '暂无'
  try {
    const d = new Date(dateStr)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return dateStr
  }
}
</script>

<style scoped>
.chat-input :deep(.el-textarea__inner) {
  border-radius: 16px;
  background-color: #f1f3f4;
  border: none;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  min-height: 52px;
}

.chat-input :deep(.el-textarea__inner:focus) {
  background-color: #fff;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.2);
}

.chat-search-input :deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: #f1f3f4;
  box-shadow: none;
  padding: 4px 16px;
}

.chat-search-input :deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.2);
}

.chat-toolbar-btn {
  padding: 8px;
  border-radius: 12px;
  transition: background-color 0.2s;
}

.chat-toolbar-btn:hover {
  background-color: #f1f3f4;
}

.chat-badge :deep(.el-badge__content.is-fixed) {
  border: 2px solid #fff;
}

.image-preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
