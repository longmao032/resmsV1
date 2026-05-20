<template>
  <div class="customer-management-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 搜索区域 (7 维度) -->
      <div class="p-6 border-b border-gray-50">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-6 gap-y-3">
          <el-form-item label="客户姓名" class="!mb-0">
            <el-input v-model="queryParams.realName" placeholder="姓名" clearable class="google-input-flat !w-36" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="手机号" class="!mb-0">
            <el-input v-model="queryParams.phone" placeholder="精确搜索" clearable class="google-input-flat !w-36" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="意向等级" class="!mb-0">
            <el-select v-model="queryParams.intentionLevel" placeholder="全部" clearable class="google-input-flat !w-28">
              <el-option label="高 (A)" :value="1" />
              <el-option label="中 (B)" :value="2" />
              <el-option label="低 (C)" :value="3" />
            </el-select>
          </el-form-item>
          <el-form-item label="客户来源" class="!mb-0">
            <el-select v-model="queryParams.source" placeholder="全部渠道" clearable class="google-input-flat !w-32">
              <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责销售" class="!mb-0">
            <el-select
              v-model="queryParams.salesId"
              placeholder="搜索销售"
              clearable
              filterable
              remote
              :remote-method="searchSalesperson"
              class="google-input-flat !w-40"
            >
              <el-option v-for="item in salespersonOptions" :key="item.id" :label="item.realName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="登记日期" class="!mb-0">
            <el-date-picker
              v-model="createTimeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              class="google-input-flat !w-56"
              @change="handleDateRangeChange"
            />
          </el-form-item>
          <el-form-item label="状态" class="!mb-0">
            <el-select v-model="queryParams.isDeleted" placeholder="正常" class="google-input-flat !w-28">
              <el-option label="正常" :value="0" />
              <el-option label="已删除" :value="1" />
            </el-select>
          </el-form-item>
          <div class="flex gap-2 items-end ml-auto">
            <el-button v-hasPermi="['trade:customer:query']" type="primary" icon="Search" class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">搜索</el-button>
            <el-button v-hasPermi="['trade:customer:query']" icon="Refresh" class="!rounded-lg px-6" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- Tab 切换 + 操作栏 -->
      <div class="px-6 py-3 flex items-center justify-between border-b border-gray-50">
        <div class="flex items-center gap-1">
          <el-radio-group v-model="queryParams.poolFilter" size="small" @change="handlePoolFilterChange">
            <el-radio-button value="all">全部客户</el-radio-button>
            <el-radio-button value="mine">我的客户</el-radio-button>
            <el-radio-button value="pool">公海池</el-radio-button>
          </el-radio-group>
          <el-divider direction="vertical" class="!mx-3" />
          <el-button v-hasPermi="['trade:customer:save']" type="primary" icon="Plus" class="!rounded-lg px-4 !bg-[#1a73e8] border-none" size="small" @click="handleAdd">登记客户</el-button>
        </div>
        <div class="flex items-center gap-4 text-sm text-gray-500">
          <div class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-red-500"></span> 高意向</div>
          <div class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-orange-400"></span> 中意向</div>
          <div class="flex items-center gap-1"><span class="w-2 h-2 rounded-full bg-blue-400"></span> 低意向</div>
        </div>
      </div>

      <!-- 表格 -->
      <div class="flex-1 px-6 pb-6 overflow-hidden">
        <el-table
          v-loading="loading"
          :data="customerList"
          row-key="id"
          height="100%"
          class="google-table-flat"
          @row-click="handlePreview"
        >
          <el-table-column label="客户信息" min-width="180">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-avatar :size="36" class="!bg-blue-100 !text-blue-600 font-bold">
                  {{ row.realName?.charAt(0) }}
                </el-avatar>
                <div class="flex flex-col">
                  <span class="font-bold text-[#1f1f1f]">{{ row.realName }}</span>
                  <span class="text-xs text-gray-400">{{ row.customerNo }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="联系电话" width="150">
            <template #default="{ row }">
              <div class="flex items-center gap-1">
                <span>{{ phoneVisibleMap[row.id] ? row.phone : maskPhone(row.phone) }}</span>
                <el-tooltip content="查看完整号码" placement="top">
                  <el-button
                    v-hasPermi="['trade:customer:view-phone']"
                    :icon="Iphone"
                    link
                    size="small"
                    class="!text-gray-400"
                    @click.stop="handleViewPhone(row)"
                  />
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="意向等级" width="110" align="center">
            <template #default="{ row }">
              <div class="flex items-center justify-center gap-2">
                <span class="w-2 h-2 rounded-full" :class="intentionColor[row.intentionLevel]"></span>
                <span class="font-medium" :class="intentionTextColor[row.intentionLevel]">
                  {{ intentionMap[row.intentionLevel] }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="购房意向" min-width="200">
            <template #default="{ row }">
              <div class="flex flex-col text-xs space-y-1">
                <span class="text-gray-600"><el-icon class="mr-1"><Location /></el-icon>{{ row.demandAreaRegion || '不限区域' }}</span>
                <span class="text-gray-400">预算: ¥{{ row.demandPrice }}万 | 面积: {{ row.demandArea }}㎡</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="客户来源" prop="source" width="110" />
          <el-table-column label="预约状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag
                v-if="row.upcomingAppointment"
                size="small"
                type="warning"
                effect="light"
                class="!rounded-full"
              >
                {{ row.upcomingAppointment }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="负责销售" prop="salesName" width="110" align="center" />
          <el-table-column label="登记日期" prop="createTime" width="150" align="center" />
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-hasPermi="['trade:customer:query']" link type="primary" @click.stop="handlePreview(row)">画像详情</el-button>
              <el-button
                v-if="queryParams.poolFilter === 'pool'"
                v-hasPermi="['trade:customer:claim']"
                link
                type="warning"
                @click.stop="handleClaim(row)"
              >领取</el-button>
              <el-button
                v-else
                v-hasPermi="['trade:customer:delete']"
                v-dataScope="{ ownerId: row.salesId }"
                link
                type="danger"
                @click.stop="handleDelete(row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="px-6 py-4 border-t border-gray-50 flex justify-end">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          class="google-pagination"
        />
      </div>
    </div>

    <!-- ========== 登记/编辑客户对话框 ========== -->
    <el-dialog
      v-model="customerDialogVisible"
      :title="customerForm.id ? '完善客户资料' : '登记新客户'"
      width="700px"
      class="google-dialog"
      destroy-on-close
    >
      <div class="px-2">
        <el-tabs v-model="activeTab" class="google-tabs">
          <el-tab-pane label="基础信息" name="basic">
            <el-form :model="customerForm" :rules="customerRules" ref="customerFormRef" label-position="top" class="mt-4">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="客户姓名" prop="realName">
                    <el-input v-model="customerForm.realName" placeholder="输入姓名" class="google-input-flat" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系电话" prop="phone">
                    <el-input v-model="customerForm.phone" placeholder="11 位手机号" maxlength="11" class="google-input-flat" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="意向等级" prop="intentionLevel">
                    <el-select v-model="customerForm.intentionLevel" placeholder="选择等级" class="google-input-flat !w-full">
                      <el-option label="高 (A)" :value="1" />
                      <el-option label="中 (B)" :value="2" />
                      <el-option label="低 (C)" :value="3" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="客户来源" prop="source">
                    <el-select v-model="customerForm.source" placeholder="来源渠道" class="google-input-flat !w-full">
                      <el-option v-for="s in sourceOptions" :key="s" :label="s" :value="s" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="身份证号" prop="idCard">
                    <el-input
                      v-model="customerForm.idCard"
                      :placeholder="customerForm.id ? '已脱敏，点击查看完整' : '可选，用于合同预填'"
                      :type="customerForm.id && !idCardEditing ? 'password' : 'text'"
                      :show-password="customerForm.id && !idCardEditing"
                      class="google-input-flat"
                      @focus="handleIdCardFocus"
                    >
                      <template v-if="customerForm.id" #suffix>
                        <el-tooltip content="查看/编辑完整身份证" placement="top">
                          <el-button v-hasPermi="['trade:customer:view-idcard']" link type="primary" :icon="Iphone" size="small" @click="handleViewIdCard(customerForm)" />
                        </el-tooltip>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="负责销售">
                    <el-select
                      v-model="customerForm.salesId"
                      placeholder="不选则进入公海池"
                      clearable
                      filterable
                      remote
                      :remote-method="searchSalesperson"
                      class="google-input-flat !w-full"
                    >
                      <el-option v-for="item in salespersonOptions" :key="item.id" :label="item.realName" :value="item.id">
                        <span>{{ item.realName }}</span>
                        <span class="text-gray-400 text-xs ml-2">{{ item.deptName || '' }}</span>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="关联 C 端账号">
                    <el-select
                      v-model="customerForm.appUserId"
                      placeholder="按手机号搜索"
                      clearable
                      filterable
                      remote
                      :remote-method="searchAppUser"
                      class="google-input-flat !w-full"
                    >
                      <el-option v-for="item in appUserOptions" :key="item.id" :label="item.phone" :value="item.id">
                        <span>{{ item.phone }}</span>
                        <span class="text-gray-400 text-xs ml-2">{{ item.nickname || '' }}</span>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="购房需求" name="demand">
            <el-form :model="customerForm" label-position="top" class="mt-4">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="意向区域">
                    <el-input v-model="customerForm.demandAreaRegion" placeholder="例如：朝阳区/望京" class="google-input-flat" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="价格预算 (万)">
                    <el-input-number v-model="customerForm.demandPrice" :min="0" :precision="2" :controls="false" class="google-input-flat !w-full" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="意向面积 (㎡)">
                    <el-input-number v-model="customerForm.demandArea" :min="0" :precision="2" :controls="false" class="google-input-flat !w-full" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="意向户型">
                    <el-select v-model="customerForm.demandLayout" placeholder="选择户型" class="google-input-flat !w-full">
                      <el-option label="1室1厅" value="1室1厅" />
                      <el-option label="2室1厅" value="2室1厅" />
                      <el-option label="3室2厅" value="3室2厅" />
                      <el-option label="4室2厅" value="4室2厅" />
                      <el-option label="别墅/豪宅" value="别墅/豪宅" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="customerDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button v-hasPermi="['trade:customer:save']" type="primary" class="!rounded-xl px-8 !bg-[#1a73e8] border-none" @click="handleSubmitCustomer">保存登记</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========== 客户画像抽屉 ========== -->
    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      size="540px"
      :with-header="false"
      class="customer-detail-drawer"
    >
      <div v-if="currentCustomer" class="h-full flex flex-col bg-white">
        <!-- 头部信息 -->
        <div class="p-8 border-b border-gray-50 bg-gradient-to-br from-[#1a73e8]/5 to-transparent">
          <div class="flex justify-between items-start mb-6">
            <div class="flex items-center gap-4">
              <el-avatar :size="64" class="!bg-white !text-[#1a73e8] shadow-sm border border-blue-100 text-2xl font-bold">
                {{ currentCustomer.realName?.charAt(0) }}
              </el-avatar>
              <div>
                <div class="flex items-center gap-2">
                  <h2 class="text-2xl font-bold text-[#202124]">{{ currentCustomer.realName }}</h2>
                  <el-tag size="small" :type="rowTagType[currentCustomer.intentionLevel]" class="!rounded-full px-3">
                    {{ intentionMap[currentCustomer.intentionLevel] }}
                  </el-tag>
                </div>
                <div class="text-sm text-gray-500 mt-1 flex items-center gap-1">
                  {{ phoneVisibleMap[currentCustomer.id] ? currentCustomer.phone : maskPhone(currentCustomer.phone) }}
                  <el-tooltip content="查看完整号码" placement="top">
                    <el-button v-hasPermi="['trade:customer:view-phone']" link type="primary" :icon="Iphone" size="small" @click.stop="handleViewPhone(currentCustomer)" />
                  </el-tooltip>
                </div>
              </div>
            </div>
            <el-button icon="Close" circle @click="drawerVisible = false" class="!border-none !bg-white shadow-sm" />
          </div>
          <!-- 统计卡片 -->
          <div class="flex gap-3">
            <div class="flex-1 bg-white/80 backdrop-blur p-3 rounded-xl border border-white shadow-sm text-center">
              <div class="text-[10px] text-gray-400 uppercase font-bold mb-1">成交次数</div>
              <div class="text-lg font-bold text-[#1a73e8]">{{ transactions.length }}</div>
            </div>
            <div class="flex-1 bg-white/80 backdrop-blur p-3 rounded-xl border border-white shadow-sm text-center">
              <div class="text-[10px] text-gray-400 uppercase font-bold mb-1">累计贡献</div>
              <div class="text-lg font-bold text-green-600">¥{{ totalContribution }}万</div>
            </div>
            <div class="flex-1 bg-white/80 backdrop-blur p-3 rounded-xl border border-white shadow-sm text-center">
              <div class="text-[10px] text-gray-400 uppercase font-bold mb-1">本月跟进</div>
              <div class="text-lg font-bold text-orange-500">{{ monthlyFollowUpCount }}</div>
            </div>
          </div>
        </div>

        <el-scrollbar class="flex-1" v-loading="loadingDetail" element-loading-text="加载客户数据...">
          <div class="p-6 space-y-6">
            <!-- 即将带看卡片 -->
            <section v-if="upcomingAppointments.length > 0">
              <h3 class="text-sm font-bold text-gray-800 mb-3 flex items-center gap-2">
                <el-icon class="text-orange-500"><Calendar /></el-icon> 即将带看
                <el-tag size="small" type="warning" effect="plain" class="!rounded-full">{{ upcomingAppointments.length }} 项</el-tag>
              </h3>
              <div class="space-y-3">
                <div
                  v-for="appt in upcomingAppointments"
                  :key="appt.id"
                  class="p-4 rounded-2xl border border-orange-200 bg-orange-50/50"
                >
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-2">
                      <el-icon class="text-orange-500" size="16"><Clock /></el-icon>
                      <span class="font-bold text-orange-700">{{ formatDateTime(appt.viewTime) }}</span>
                    </div>
                    <span class="text-xs text-orange-400">{{ countdownText(appt.viewTime) }}</span>
                  </div>
                  <div class="text-sm text-gray-600 mb-1">
                    {{ appt.houseTitle || '待选房源' }}
                    <span v-if="appt.houseAddress" class="text-gray-400"> · {{ appt.houseAddress }}</span>
                  </div>
                  <p v-if="appt.customerFeedback" class="text-xs text-gray-500 mt-1">备注: {{ appt.customerFeedback }}</p>
                  <div class="flex gap-2 mt-3">
                    <el-button size="small" type="success" class="!rounded-lg" @click.stop="openCompleteAppointment(appt)">确认完成</el-button>
                    <el-button size="small" class="!rounded-lg" @click.stop="openCancelAppointment(appt)">取消预约</el-button>
                  </div>
                </div>
              </div>
            </section>

            <!-- C端账号信息 -->
            <section v-if="appUserInfo">
              <h3 class="text-sm font-bold text-gray-800 mb-3 flex items-center gap-2">
                <el-icon class="text-green-500"><User /></el-icon> C 端账号信息
              </h3>
              <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                <div class="flex items-center gap-3 mb-3">
                  <el-avatar :size="40" :src="appUserInfo.avatarUrl" class="!bg-gray-200">
                    {{ appUserInfo.nickname?.charAt(0) || '?' }}
                  </el-avatar>
                  <div>
                    <div class="font-medium">{{ appUserInfo.nickname || '未设置昵称' }}</div>
                    <div class="text-xs text-gray-400">{{ appUserInfo.phone }}</div>
                  </div>
                  <el-tag v-if="appUserInfo.wechatOpenid" size="small" type="success" effect="plain" class="!rounded-full ml-auto">微信已绑定</el-tag>
                </div>
                <div class="text-xs text-gray-400">注册时间: {{ appUserInfo.createTime || '未知' }}</div>
              </div>
            </section>

            <!-- 购房需求 DNA -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]"><Compass /></el-icon> 客户需求 DNA
              </h3>
              <div class="grid grid-cols-2 gap-3">
                <div class="p-3 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">意向区域</div>
                  <div class="font-medium text-gray-800">{{ currentCustomer.demandAreaRegion || '不限' }}</div>
                </div>
                <div class="p-3 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">意向面积</div>
                  <div class="font-medium text-gray-800">{{ currentCustomer.demandArea || '-' }} ㎡</div>
                </div>
                <div class="p-3 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">价格预算</div>
                  <div class="font-medium text-gray-800">¥{{ currentCustomer.demandPrice || '-' }} 万</div>
                </div>
                <div class="p-3 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="text-xs text-gray-400 mb-1">意向户型</div>
                  <div class="font-medium text-gray-800">{{ currentCustomer.demandLayout || '不限' }}</div>
                </div>
              </div>
            </section>

            <!-- 历史成交订单 -->
            <section v-if="transactions.length > 0">
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]"><Files /></el-icon> 历史成交订单
              </h3>
              <div class="space-y-3">
                <div
                  v-for="order in transactions" :key="order.id"
                  class="p-4 rounded-2xl border border-gray-100 hover:border-[#1a73e8] hover:bg-blue-50/30 transition-all cursor-pointer group"
                >
                  <div class="flex justify-between items-center mb-2">
                    <span class="text-xs font-mono text-[#1a73e8] font-medium">{{ order.transactionNo }}</span>
                    <el-tag size="small" :type="orderStatusTag(order.status)" effect="plain" class="!rounded-md">
                      {{ orderStatusMap[order.status] }}
                    </el-tag>
                  </div>
                  <div class="font-bold text-gray-700 group-hover:text-[#1a73e8] text-sm">{{ order.houseAddress || order.house?.projectName || '' }}</div>
                  <div class="flex justify-between mt-2 text-xs text-gray-400">
                    <span>成交价: ¥{{ (order.dealPrice / 10000).toFixed(2) }}万</span>
                    <span>{{ order.createTime }}</span>
                  </div>
                  <!-- 付款进度 -->
                  <div v-if="order.paymentProgress" class="mt-2">
                    <el-progress :percentage="order.paymentProgress" :stroke-width="4" size="small" />
                    <div class="text-xs text-gray-400 mt-1">已收 {{ order.paidAmount || 0 }}/{{ order.receivableAmount || 0 }} 万元</div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 跟进足迹 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]"><ChatLineSquare /></el-icon> 跟进足迹
                <span class="text-xs text-gray-400 font-normal ml-1">共 {{ followUpRecords.length }} 条</span>
              </h3>

              <!-- 进行中 -->
              <div v-if="activeFollowUps.length > 0" class="mb-4">
                <div class="text-xs text-gray-400 font-bold mb-2 flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-green-400 inline-block"></span> 进行中
                </div>
                <el-timeline class="ml-1">
                  <el-timeline-item
                    v-for="item in activeFollowUps"
                    :key="item.id"
                    :timestamp="item.viewTime || item.createTime"
                    type="warning"
                    color="#f59e0b"
                  >
                    <div class="flex items-center gap-2">
                      <span class="text-sm font-bold text-gray-700">{{ followUpTypeLabel(item.type) }}</span>
                      <el-tag size="small" type="warning" effect="plain" class="!rounded-full">已预约</el-tag>
                      <span v-if="item.houseTitle" class="text-xs text-[#1a73e8]"> · {{ item.houseTitle }}</span>
                    </div>
                    <div class="flex gap-2 mt-2">
                      <el-button size="small" type="success" class="!rounded-lg !h-7" @click="openCompleteAppointment(item)">确认完成</el-button>
                      <el-button size="small" class="!rounded-lg !h-7" @click="openCancelAppointment(item)">取消</el-button>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>

              <!-- 历史记录 -->
              <div v-if="historyFollowUps.length > 0">
                <div class="text-xs text-gray-400 font-bold mb-2 flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-gray-300 inline-block"></span> 历史记录
                </div>
                <el-timeline class="ml-1">
                  <el-timeline-item
                    v-for="item in historyFollowUps"
                    :key="item.id"
                    :timestamp="item.viewTime || item.createTime"
                    :type="item.type === 'visit' ? 'primary' : 'info'"
                  >
                    <div class="flex items-center gap-2">
                      <span class="text-sm font-bold text-gray-700">{{ followUpTypeLabel(item.type) }}</span>
                      <span v-if="item.status === 3" class="text-xs text-red-500 bg-red-50 px-2 rounded">已取消</span>
                      <span v-if="item.houseTitle" class="text-xs font-normal text-[#1a73e8]"> · {{ item.houseTitle }}</span>
                    </div>
                    <p v-if="item.customerFeedback" class="text-xs text-gray-500 mt-1">客户反馈: {{ item.customerFeedback }}</p>
                    <p v-if="item.content" class="text-xs text-gray-400 mt-0.5">销售备注: {{ item.content }}</p>
                    <p v-if="item.followAdvice" class="text-xs text-blue-500 mt-1">下一步: {{ item.followAdvice }}</p>
                    <p v-if="item.cancelReason" class="text-xs text-red-400 mt-1">取消原因: {{ item.cancelReason }}</p>
                    <p v-if="item.newIntentionLevel" class="text-xs text-orange-500 mt-1">
                      意向调整 → {{ intentionMap[item.newIntentionLevel] }}
                    </p>
                  </el-timeline-item>
                </el-timeline>
              </div>

              <div v-if="followUpRecords.length === 0 && !loadingDetail" class="text-sm text-gray-400 py-8 text-center">
                暂无跟进记录
              </div>
            </section>
          </div>
        </el-scrollbar>

        <!-- 底部按钮 -->
        <div class="p-6 border-t border-gray-50 flex gap-3">
          <el-button v-hasPermi="['trade:customer:edit']" v-dataScope="{ ownerId: currentCustomer.salesId }" class="flex-1 !rounded-xl !h-11" icon="Edit" @click="handleEdit(currentCustomer)">完善资料</el-button>
          <el-button
            v-hasPermi="['trade:customer:appointment']"
            v-dataScope="{ ownerId: currentCustomer.salesId }"
            class="flex-1 !rounded-xl !h-11"
            icon="Calendar"
            type="warning"
            @click="openAppointmentDialog"
          >预约带看</el-button>
          <el-button v-hasPermi="['trade:customer:followup']" v-dataScope="{ ownerId: currentCustomer.salesId }" type="primary" class="flex-1 !rounded-xl !h-11 !bg-[#1a73e8] border-none" icon="Plus" @click="handleOpenFollowUp">新增跟进</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- ========== 新增跟进/记录对话框 ========== -->
    <el-dialog
      v-model="followUpDialogVisible"
      title="记录跟进足迹"
      width="580px"
      class="google-dialog"
      destroy-on-close
    >
      <div class="px-2">
        <el-form :model="followUpForm" :rules="followUpRules" ref="followUpFormRef" label-position="top">
          <el-form-item label="行为类型" prop="type">
            <el-radio-group v-model="followUpForm.type" class="google-radio-group flex flex-wrap gap-2">
              <el-radio-button value="visit">实地带看</el-radio-button>
              <el-radio-button value="call">电话咨询</el-radio-button>
              <el-radio-button value="wechat">微信沟通</el-radio-button>
              <el-radio-button value="other">其他</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="followUpForm.type === 'visit'" label="带看房源" prop="houseId">
            <el-select
              v-model="followUpForm.houseId"
              placeholder="搜索项目/房号"
              filterable
              remote
              :remote-method="searchHouse"
              class="google-input-flat !w-full"
            >
              <el-option v-for="item in houseOptions" :key="item.id" :label="`${item.projectName || ''} - ${item.houseNo || ''}`" :value="item.id">
                <span>{{ item.projectName }} - {{ item.houseNo }}</span>
                <span class="text-gray-400 text-xs ml-2">{{ item.roomNo || '' }}</span>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="客户反馈">
            <el-input
              v-model="followUpForm.customerFeedback"
              type="textarea"
              :rows="2"
              placeholder="记录客户原话、关注点或反馈意见..."
              class="google-input-flat"
            />
          </el-form-item>

          <el-form-item label="销售备注">
            <el-input
              v-model="followUpForm.content"
              type="textarea"
              :rows="2"
              placeholder="内部工作记录..."
              class="google-input-flat"
            />
          </el-form-item>

          <el-form-item label="跟进建议（下一步计划）">
            <el-input
              v-model="followUpForm.followAdvice"
              type="textarea"
              :rows="2"
              placeholder="例如：下次约看其他楼盘、准备贷款材料..."
              class="google-input-flat"
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="是否调整意向">
                <el-select v-model="followUpForm.adjustIntention" class="google-input-flat !w-full">
                  <el-option label="不调整" :value="false" />
                  <el-option label="调整等级" :value="true" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item v-if="followUpForm.adjustIntention" label="调整至">
                <el-select v-model="followUpForm.newLevel" class="google-input-flat !w-full">
                  <el-option label="高 (A)" :value="1" />
                  <el-option label="中 (B)" :value="2" />
                  <el-option label="低 (C)" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item v-else label="跟进时间">
                <el-date-picker
                  v-model="followUpForm.date"
                  type="datetime"
                  format="YYYY-MM-DD HH:mm"
                  placeholder="选择时间"
                  class="google-input-flat !w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row v-if="followUpForm.type === 'visit'" :gutter="20">
            <el-col :span="12">
              <el-form-item label="带看结果">
                <el-select v-model="followUpForm.status" class="google-input-flat !w-full">
                  <el-option label="已完成" :value="2" />
                  <el-option label="已取消" :value="3" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item v-if="followUpForm.status === 3" label="取消原因" prop="cancelReason">
                <el-input v-model="followUpForm.cancelReason" placeholder="客户临时有事/改变主意..." class="google-input-flat" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="followUpDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button v-hasPermi="['trade:customer:followup']" type="primary" class="!rounded-xl px-8 !bg-[#1a73e8] border-none" @click="handleSubmitFollowUp">提交记录</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========== 预约带看对话框 ========== -->
    <el-dialog
      v-model="appointmentDialogVisible"
      title="预约带看"
      width="520px"
      class="google-dialog"
      destroy-on-close
    >
      <div class="px-2">
        <el-form :model="appointmentForm" :rules="appointmentRules" ref="appointmentFormRef" label-position="top">
          <el-form-item label="预约时间" prop="viewTime">
            <el-date-picker
              v-model="appointmentForm.viewTime"
              type="datetime"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择带看时间"
              :disabled-date="disablePastDate"
              class="google-input-flat !w-full"
            />
          </el-form-item>
          <el-form-item label="带看房源" prop="houseId">
            <el-select
              v-model="appointmentForm.houseId"
              placeholder="搜索项目/房号"
              filterable
              remote
              :remote-method="searchHouse"
              class="google-input-flat !w-full"
            >
              <el-option v-for="item in houseOptions" :key="item.id" :label="`${item.projectName || ''} - ${item.houseNo || ''}`" :value="item.id">
                <span>{{ item.projectName }} - {{ item.houseNo }}</span>
                <span class="text-gray-400 text-xs ml-2">{{ item.address || item.roomNo || '' }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="客户关注点">
            <el-input
              v-model="appointmentForm.customerFeedback"
              type="textarea"
              :rows="3"
              placeholder="记录客户关注的问题，方便带看时重点介绍..."
              class="google-input-flat"
            />
          </el-form-item>
          <el-form-item label="意向调整">
            <el-select v-model="appointmentForm.adjustIntention" class="google-input-flat !w-full" @change="appointmentForm.newLevel = undefined">
              <el-option label="不调整" :value="false" />
              <el-option label="调整等级" :value="true" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="appointmentForm.adjustIntention" label="调整至">
            <el-select v-model="appointmentForm.newLevel" class="google-input-flat !w-full">
              <el-option label="高 (A)" :value="1" />
              <el-option label="中 (B)" :value="2" />
              <el-option label="低 (C)" :value="3" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="appointmentDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button v-hasPermi="['trade:customer:appointment']" type="primary" class="!rounded-xl px-8 !bg-orange-500 border-none" @click="handleSubmitAppointment">创建预约</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========== 确认完成预约对话框 ========== -->
    <el-dialog
      v-model="completeDialogVisible"
      title="确认完成带看"
      width="450px"
      class="google-dialog"
      destroy-on-close
    >
      <div class="px-2">
        <el-form :model="completeForm" ref="completeFormRef" label-position="top">
          <el-form-item label="客户反馈">
            <el-input
              v-model="completeForm.customerFeedback"
              type="textarea"
              :rows="3"
              placeholder="记录客户对房源的反馈意见..."
              class="google-input-flat"
            />
          </el-form-item>
          <el-form-item label="跟进建议">
            <el-input
              v-model="completeForm.followAdvice"
              type="textarea"
              :rows="2"
              placeholder="下一步计划..."
              class="google-input-flat"
            />
          </el-form-item>
          <el-form-item label="是否调整意向">
            <el-select v-model="completeForm.adjustIntention" class="google-input-flat !w-full">
              <el-option label="不调整" :value="false" />
              <el-option label="调整等级" :value="true" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="completeForm.adjustIntention" label="调整至">
            <el-select v-model="completeForm.newLevel" class="google-input-flat !w-full">
              <el-option label="高 (A)" :value="1" />
              <el-option label="中 (B)" :value="2" />
              <el-option label="低 (C)" :value="3" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="completeDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button type="success" class="!rounded-xl px-8" @click="handleSubmitComplete">确认完成</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- ========== 取消预约对话框 ========== -->
    <el-dialog
      v-model="cancelDialogVisible"
      title="取消预约"
      width="450px"
      class="google-dialog"
      destroy-on-close
    >
      <div class="px-2">
        <el-form :model="cancelForm" ref="cancelFormRef" label-position="top">
          <el-form-item label="取消原因" prop="cancelReason" :rules="[{ required: true, message: '请填写取消原因', trigger: 'blur' }]">
            <el-input
              v-model="cancelForm.cancelReason"
              type="textarea"
              :rows="3"
              placeholder="请说明取消原因..."
              class="google-input-flat"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="cancelDialogVisible = false" class="!rounded-xl px-6">返回</el-button>
          <el-button type="danger" class="!rounded-xl px-8" @click="handleSubmitCancel">确认取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import {
  Search, Refresh, Plus, UserFilled, Location, Close, Compass,
  Files, ChatLineSquare, Edit, Iphone, Calendar, Clock, User
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCustomer, saveCustomer, delCustomer, claimCustomer, getCustomerPhone, getCustomerIdCard,
  listFollowUp, saveFollowUp, updateFollowUp,
  createAppointment, completeAppointment, cancelAppointment, listAppointments,
  getCustomerStats
} from '@/api/trade/customer'
import { listOrder } from '@/api/trade/order'
import { getSalesOptions } from '@/api/system/user'
import { listAppUser } from '@/api/trade/appuser'
import { listHouse } from '@/api/house/house'

// ========== 类型 ==========
interface Customer {
  id: number
  customerNo: string
  realName: string
  phone: string
  idCard?: string
  salesId?: number
  salesName?: string
  demandArea: number
  demandPrice: number
  demandLayout?: string
  demandAreaRegion?: string
  intentionLevel: number
  source: string
  appUserId?: number
  createTime: string
  upcomingAppointment?: string
}

// ========== 映射 ==========
const intentionMap: Record<number, string> = { 1: '高意向 (A)', 2: '中意向 (B)', 3: '低意向 (C)' }
const intentionColor: Record<number, string> = { 1: 'bg-red-500', 2: 'bg-orange-400', 3: 'bg-blue-400' }
const intentionTextColor: Record<number, string> = { 1: 'text-red-600', 2: 'text-orange-500', 3: 'text-blue-500' }
const rowTagType: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'info' }
const orderStatusMap: Record<number, string> = {
  0: '待付定金', 1: '已付定金', 2: '已付首付', 3: '已过户', 4: '已完成', 5: '已取消'
}
const orderStatusTag: (s: number) => string = (s) => {
  const map: Record<number, string> = { 0: 'danger', 1: 'warning', 2: 'warning', 3: 'primary', 4: 'success', 5: 'info' }
  return map[s] || 'info'
}

const sourceOptions = [
  '官网预约', '转介绍', '线下到访', '广告投放',
  '抖音/快手', '朋友圈', '老客户推荐', '其他'
]

// ========== 状态 ==========
const loading = ref(false)
const total = ref(0)
const customerList = ref<Customer[]>([])
const currentCustomer = ref<Customer | null>(null)
const drawerVisible = ref(false)
const transactions = ref<any[]>([])
const followUpRecords = ref<any[]>([])
const upcomingAppointments = ref<any[]>([])
const appUserInfo = ref<any>(null)
const loadingDetail = ref(false)

const salespersonOptions = ref<any[]>([])
const houseOptions = ref<any[]>([])
const appUserOptions = ref<any[]>([])

const phoneVisibleMap = reactive<Record<number, boolean>>({})
const realPhoneCache = reactive<Record<number, string>>({})
const realIdCardCache = reactive<Record<number, string>>({})
const idCardEditing = ref(false)

const createTimeRange = ref<any>(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  realName: '',
  phone: '',
  intentionLevel: undefined as number | undefined,
  source: '',
  salesId: undefined as number | undefined,
  createTimeBegin: '',
  createTimeEnd: '',
  isDeleted: 0,
  poolFilter: 'all'
})

// ========== 工具方法 ==========
const maskPhone = (phone: string) => {
  if (!phone || phone.length < 7) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

const formatDateTime = (dt: string) => {
  if (!dt) return ''
  return dt.slice(0, 16).replace('T', ' ')
}

const countdownText = (dateStr: string) => {
  const target = new Date(dateStr).getTime()
  const now = Date.now()
  const diff = target - now
  if (diff < 0) return '已过期'
  const days = Math.floor(diff / 86400000)
  const hours = Math.floor((diff % 86400000) / 3600000)
  if (days > 0) return `距预约还有 ${days} 天`
  if (hours > 0) return `距预约还有 ${hours} 小时`
  return '即将开始'
}

const disablePastDate = (date: Date) => date < new Date()

// ========== 计算属性 ==========
const activeFollowUps = computed(() =>
  followUpRecords.value.filter((r: any) => r.status === 1 || r.status === 0)
)
const historyFollowUps = computed(() =>
  followUpRecords.value.filter((r: any) => r.status === 2 || r.status === 3)
)
const monthlyFollowUpCount = computed(() => {
  const now = new Date()
  const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
  return followUpRecords.value.filter((r: any) => {
    const d = new Date(r.createTime || r.viewTime)
    return d >= monthStart
  }).length
})
const totalContribution = computed(() => {
  if (!transactions.value.length) return 0
  return transactions.value.reduce((sum: number, item: any) => sum + (item.dealPrice || 0) / 10000, 0).toFixed(2)
})

// ========== 搜索区方法 ==========
const handleDateRangeChange = (val: [string, string] | null) => {
  if (val) {
    queryParams.createTimeBegin = val[0]
    queryParams.createTimeEnd = val[1]
  } else {
    queryParams.createTimeBegin = ''
    queryParams.createTimeEnd = ''
  }
}

const handlePoolFilterChange = () => {
  queryParams.pageNum = 1
  getList()
}

const searchSalesperson = async (query: string) => {
  if (!query) {
    salespersonOptions.value = []
    return
  }
  try {
    const res: any = await getSalesOptions(query)
    salespersonOptions.value = res.data || []
  } catch { salespersonOptions.value = [] }
}

const searchHouse = async (query: string) => {
  if (!query) {
    houseOptions.value = []
    return
  }
  try {
    const res: any = await listHouse({ keyword: query, pageNum: 1, pageSize: 20 })
    houseOptions.value = res.data?.records || []
  } catch { houseOptions.value = [] }
}

const searchAppUser = async (query: string) => {
  if (!query) {
    appUserOptions.value = []
    return
  }
  try {
    const res: any = await listAppUser({ phone: query, pageNum: 1, pageSize: 10 })
    appUserOptions.value = res.data?.records || []
  } catch { appUserOptions.value = [] }
}

// ========== 列表方法 ==========
const getList = async () => {
  loading.value = true
  try {
    const response: any = await listCustomer(queryParams)
    customerList.value = response.data.records
    total.value = response.data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryParams.realName = ''
  queryParams.phone = ''
  queryParams.intentionLevel = undefined
  queryParams.source = ''
  queryParams.salesId = undefined
  queryParams.createTimeBegin = ''
  queryParams.createTimeEnd = ''
  queryParams.isDeleted = 0
  createTimeRange.value = null
  handleQuery()
}

getList()

// ========== 客户表单 ==========
const customerDialogVisible = ref(false)
const activeTab = ref('basic')
const customerFormRef = ref()
const customerForm = reactive({
  id: undefined as number | undefined,
  realName: '',
  phone: '',
  idCard: '',
  intentionLevel: 2,
  source: '',
  salesId: undefined as number | undefined,
  appUserId: undefined as number | undefined,
  demandAreaRegion: '',
  demandPrice: 0,
  demandArea: 0,
  demandLayout: ''
})

const customerRules = reactive({
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入电话', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  intentionLevel: [{ required: true, message: '请选择意向等级', trigger: 'change' }],
  idCard: [
    { pattern: /^\d{15}$|^\d{17}[\dXx]$/, message: '请输入正确的身份证号', trigger: 'blur' }
  ]
})

const handleAdd = () => {
  resetCustomerForm()
  customerDialogVisible.value = true
}

const handleEdit = (customer: Customer) => {
  resetCustomerForm()
  Object.assign(customerForm, customer)
  customerDialogVisible.value = true
}

const resetCustomerForm = () => {
  customerForm.id = undefined
  customerForm.realName = ''
  customerForm.phone = ''
  customerForm.idCard = ''
  customerForm.intentionLevel = 2
  customerForm.source = ''
  customerForm.salesId = undefined
  customerForm.appUserId = undefined
  customerForm.demandAreaRegion = ''
  customerForm.demandPrice = 0
  customerForm.demandArea = 0
  customerForm.demandLayout = ''
  activeTab.value = 'basic'
  idCardEditing.value = false
}

const handleIdCardFocus = () => {
  if (customerForm.id && !idCardEditing.value) {
    idCardEditing.value = true
    customerForm.idCard = ''
  }
}

const handleSubmitCustomer = () => {
  customerFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      try {
        await saveCustomer(customerForm)
        ElMessage.success(customerForm.id ? '修改成功' : '新增成功')
        customerDialogVisible.value = false
        getList()
      } catch { /* handled by interceptor */ }
    }
  })
}

const handleDelete = (row: Customer) => {
  ElMessageBox.confirm('是否确认删除该客户？', '警告', {
    confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    await delCustomer(row.id)
    ElMessage.success('删除成功')
    getList()
  })
}

const handleClaim = async (row: Customer) => {
  try {
    await claimCustomer(row.id)
    ElMessage.success('已领取该客户')
    getList()
  } catch { /* handled */ }
}

// ========== 敏感数据 ==========
const handleViewPhone = async (customer: Customer) => {
  if (realPhoneCache[customer.id]) {
    phoneVisibleMap[customer.id] = true
    ElMessage.info(`完整号码: ${realPhoneCache[customer.id]}`)
    return
  }
  try {
    const res: any = await getCustomerPhone(customer.id)
    realPhoneCache[customer.id] = res.data || res
    phoneVisibleMap[customer.id] = true
    ElMessage.success(`完整号码: ${realPhoneCache[customer.id]}`)
  } catch { /* handled */ }
}

const handleViewIdCard = async (customer: Customer) => {
  if (realIdCardCache[customer.id]) {
    customerForm.idCard = realIdCardCache[customer.id]
    idCardEditing.value = true
    ElMessage.info(`身份证号: ${realIdCardCache[customer.id]}`)
    return
  }
  try {
    const res: any = await getCustomerIdCard(customer.id)
    realIdCardCache[customer.id] = res.data || res
    customerForm.idCard = realIdCardCache[customer.id]
    idCardEditing.value = true
    ElMessage.success(`身份证号: ${realIdCardCache[customer.id]}`)
  } catch { /* handled */ }
}

// ========== 画像抽屉 ==========
const handlePreview = async (row: Customer) => {
  currentCustomer.value = row
  drawerVisible.value = true
  loadingDetail.value = true
  transactions.value = []
  followUpRecords.value = []
  upcomingAppointments.value = []
  appUserInfo.value = null
  try {
    const [orderRes, followRes, apptRes] = await Promise.all([
      listOrder({ customerId: row.id, pageNum: 1, pageSize: 50 }),
      listFollowUp(row.id),
      listAppointments(row.id)
    ])
    transactions.value = orderRes.data?.records || []
    followUpRecords.value = followRes.data || []
    upcomingAppointments.value = apptRes.data || []

    // 加载 C 端账号信息
    if (row.appUserId) {
      const appRes: any = await listAppUser({ id: row.appUserId, pageNum: 1, pageSize: 1 })
      const users = appRes.data?.records || []
      if (users.length > 0) appUserInfo.value = users[0]
    }
  } catch {
    transactions.value = []
    followUpRecords.value = []
    upcomingAppointments.value = []
  } finally {
    loadingDetail.value = false
  }
}

// ========== 跟进记录 ==========
const followUpDialogVisible = ref(false)
const followUpFormRef = ref()
const followUpForm = reactive({
  type: 'visit',
  houseId: undefined as number | undefined,
  customerFeedback: '',
  content: '',
  followAdvice: '',
  adjustIntention: false,
  newLevel: undefined as number | undefined,
  status: 2,
  cancelReason: '',
  date: new Date()
})

const followUpRules = {
  type: [{ required: true, message: '请选择行为类型', trigger: 'change' }],
  cancelReason: [{ required: true, message: '已取消的带看请填写原因', trigger: 'blur' }]
}

const handleOpenFollowUp = () => {
  followUpForm.type = 'visit'
  followUpForm.houseId = undefined
  followUpForm.customerFeedback = ''
  followUpForm.content = ''
  followUpForm.followAdvice = ''
  followUpForm.adjustIntention = false
  followUpForm.newLevel = undefined
  followUpForm.status = 2
  followUpForm.cancelReason = ''
  followUpForm.date = new Date()
  followUpDialogVisible.value = true
}

const handleSubmitFollowUp = () => {
  followUpFormRef.value.validate(async (valid: boolean) => {
    if (valid && currentCustomer.value) {
      if (followUpForm.type === 'visit' && followUpForm.status === 3 && !followUpForm.cancelReason) {
        ElMessage.warning('已取消的带看请填写取消原因')
        return
      }
      try {
        const isVisit = followUpForm.type === 'visit'
        const data = {
          type: followUpForm.type,
          houseId: isVisit ? followUpForm.houseId : undefined,
          customerFeedback: followUpForm.customerFeedback || undefined,
          content: followUpForm.content || undefined,
          followAdvice: followUpForm.followAdvice || undefined,
          newIntentionLevel: followUpForm.adjustIntention ? followUpForm.newLevel : undefined,
          followDate: followUpForm.date,
          status: isVisit ? followUpForm.status : 2,
          cancelReason: isVisit && followUpForm.status === 3 ? followUpForm.cancelReason : undefined
        }
        await saveFollowUp(currentCustomer.value.id, data)
        ElMessage.success('跟进记录已保存')
        followUpDialogVisible.value = false
        // 刷新
        const [followRes, apptRes] = await Promise.all([
          listFollowUp(currentCustomer.value.id),
          listAppointments(currentCustomer.value.id)
        ])
        followUpRecords.value = followRes.data || []
        upcomingAppointments.value = apptRes.data || []
        if (data.newIntentionLevel) getList()
      } catch { /* handled */ }
    }
  })
}

// ========== 预约带看 ==========
const appointmentDialogVisible = ref(false)
const appointmentFormRef = ref()
const appointmentForm = reactive({
  viewTime: '',
  houseId: undefined as number | undefined,
  customerFeedback: '',
  adjustIntention: false,
  newLevel: undefined as number | undefined
})

const appointmentRules = {
  viewTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }]
}

const openAppointmentDialog = () => {
  appointmentForm.viewTime = ''
  appointmentForm.houseId = undefined
  appointmentForm.customerFeedback = ''
  appointmentForm.adjustIntention = false
  appointmentForm.newLevel = undefined
  appointmentDialogVisible.value = true
}

const handleSubmitAppointment = () => {
  appointmentFormRef.value.validate(async (valid: boolean) => {
    if (valid && currentCustomer.value) {
      try {
        await createAppointment(currentCustomer.value.id, {
          viewTime: appointmentForm.viewTime,
          houseId: appointmentForm.houseId,
          customerFeedback: appointmentForm.customerFeedback || undefined,
          newIntentionLevel: appointmentForm.adjustIntention ? appointmentForm.newLevel : undefined
        })
        ElMessage.success('预约创建成功')
        appointmentDialogVisible.value = false
        // 刷新
        const [followRes, apptRes] = await Promise.all([
          listFollowUp(currentCustomer.value.id),
          listAppointments(currentCustomer.value.id)
        ])
        followUpRecords.value = followRes.data || []
        upcomingAppointments.value = apptRes.data || []
      } catch { /* handled */ }
    }
  })
}

// ========== 确认完成预约 ==========
const completeDialogVisible = ref(false)
const completeFormRef = ref()
const completeForm = reactive({
  currentAppt: null as any,
  customerFeedback: '',
  followAdvice: '',
  adjustIntention: false,
  newLevel: undefined as number | undefined
})

const openCompleteAppointment = (appt: any) => {
  completeForm.currentAppt = appt
  completeForm.customerFeedback = ''
  completeForm.followAdvice = ''
  completeForm.adjustIntention = false
  completeForm.newLevel = undefined
  completeDialogVisible.value = true
}

const handleSubmitComplete = async () => {
  if (!completeForm.currentAppt || !currentCustomer.value) return
  try {
    await completeAppointment(completeForm.currentAppt.id, {
      customerFeedback: completeForm.customerFeedback || undefined,
      followAdvice: completeForm.followAdvice || undefined,
      newIntentionLevel: completeForm.adjustIntention ? completeForm.newLevel : undefined
    })
    ElMessage.success('带看已完成')
    completeDialogVisible.value = false
    const [followRes, apptRes] = await Promise.all([
      listFollowUp(currentCustomer.value.id),
      listAppointments(currentCustomer.value.id)
    ])
    followUpRecords.value = followRes.data || []
    upcomingAppointments.value = apptRes.data || []
    if (completeForm.adjustIntention) getList()
  } catch { /* handled */ }
}

// ========== 取消预约 ==========
const cancelDialogVisible = ref(false)
const cancelFormRef = ref()
const cancelForm = reactive({
  currentAppt: null as any,
  cancelReason: ''
})

const openCancelAppointment = (appt: any) => {
  cancelForm.currentAppt = appt
  cancelForm.cancelReason = ''
  cancelDialogVisible.value = true
}

const handleSubmitCancel = async () => {
  if (!cancelForm.currentAppt || !currentCustomer.value) return
  try {
    await cancelAppointment(cancelForm.currentAppt.id, {
      cancelReason: cancelForm.cancelReason
    })
    ElMessage.success('预约已取消')
    cancelDialogVisible.value = false
    const [followRes, apptRes] = await Promise.all([
      listFollowUp(currentCustomer.value.id),
      listAppointments(currentCustomer.value.id)
    ])
    followUpRecords.value = followRes.data || []
    upcomingAppointments.value = apptRes.data || []
  } catch { /* handled */ }
}

// ========== 工具方法 ==========
const followUpTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    visit: '实地带看', call: '电话咨询', wechat: '微信沟通', other: '其他跟进'
  }
  return map[type] || type
}
</script>

<style scoped>
.customer-management-container {
  height: calc(100vh - 110px);
}

.google-table-flat {
  --el-table-header-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f8f9fa;
  --el-table-border: none;
}

:deep(.el-table__header th) {
  color: #5f6368;
  font-weight: 600;
  border-bottom: 1px solid #f1f3f4 !important;
}

:deep(.el-table__row) {
  height: 72px;
  cursor: pointer;
}

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}

:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 8px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
}

:deep(.google-input-flat .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

:deep(.google-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

:deep(.customer-detail-drawer .el-drawer__body) {
  padding: 0;
}

:deep(.el-timeline-item__tail) {
  border-left: 2px dashed #e8eaed;
}

:deep(.google-dialog) {
  border-radius: 28px;
  overflow: hidden;
}

:deep(.google-dialog .el-dialog__header) {
  padding: 24px 32px 16px;
  margin-right: 0;
  border-bottom: 1px solid #f1f3f4;
}

:deep(.google-dialog .el-dialog__title) {
  font-size: 20px;
  font-weight: bold;
  color: #202124;
}

:deep(.google-dialog .el-dialog__body) {
  padding: 24px 32px;
}

:deep(.google-tabs .el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.google-tabs .el-tabs__active-bar) {
  background-color: #1a73e8;
  height: 3px;
  border-radius: 3px;
}

:deep(.google-tabs .el-tabs__item.is-active) {
  color: #1a73e8;
  font-weight: bold;
}

:deep(.google-tabs .el-tabs__item) {
  font-size: 15px;
}

:deep(.google-radio-group .el-radio-button__inner) {
  border-radius: 12px !important;
  border: 1px solid #f1f3f4 !important;
  background-color: #f8f9fa !important;
  color: #5f6368 !important;
  margin-right: 8px;
  padding: 10px 20px;
  box-shadow: none !important;
}

:deep(.google-radio-group .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #1a73e8 !important;
  color: #fff !important;
  border-color: #1a73e8 !important;
}
</style>
