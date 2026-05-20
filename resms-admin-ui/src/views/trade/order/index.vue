<template>
  <div class="order-management-container p-6 bg-gray-50 min-h-full">
    <div class="flex flex-col h-full bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
      <!-- 搜索区域 -->
      <div class="p-6 border-b border-gray-50">
        <el-form :inline="true" :model="queryParams" class="flex flex-wrap gap-x-8 gap-y-4">
          <el-form-item label="交易编号" class="!mb-0">
            <el-input v-model="queryParams.transactionNo" placeholder="TRX-2026..." clearable
              class="google-input-flat !w-48" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="客户姓名" class="!mb-0">
            <el-input v-model="queryParams.customerName" placeholder="客户姓名" clearable class="google-input-flat !w-40"
              @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="交易状态" class="!mb-0">
            <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="google-input-flat !w-36">
              <el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="Number(value)" />
            </el-select>
          </el-form-item>
          <el-form-item label="交易时间" class="!mb-0">
            <el-date-picker v-model="queryParams.dateRange" type="daterange" range-separator="-"
              start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
              class="google-input-flat !w-64" />
          </el-form-item>
          <div class="flex gap-2 ml-auto">
            <el-button v-hasPermi="['trade:order:query']" type="primary" icon="Search"
              class="!rounded-lg !bg-[#1a73e8] border-none px-6" @click="handleQuery">查询</el-button>
            <el-button v-hasPermi="['trade:order:query']" icon="Refresh" class="!rounded-lg px-6"
              @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="px-6 py-4 flex items-center justify-between">
        <div class="flex gap-3">
          <el-button v-hasPermi="['trade:order:add']" type="primary" icon="Plus"
            class="!rounded-lg px-5 !bg-[#1a73e8] border-none" @click="handleAdd">新建订单</el-button>
          <el-button v-hasPermi="['trade:order:export']" plain icon="Download" class="!rounded-lg px-5" @click="handleExport">导出数据</el-button>
        </div>
        <div class="flex gap-1 text-gray-400 text-sm">
          共 <span class="font-bold text-[#1a73e8] mx-1">{{ total }}</span> 笔交易订单
        </div>
      </div>

      <!-- 表格区域 -->
      <div class="flex-1 px-6 pb-6 overflow-hidden">
        <el-table v-loading="loading" :data="orderList" row-key="id" height="100%" class="google-table-flat"
          @row-click="handlePreview">
          <el-table-column label="交易编号" prop="transactionNo" width="160">
            <template #default="{ row }">
              <span class="font-mono text-[#1a73e8] font-medium cursor-pointer hover:underline">{{ row.transactionNo
                }}</span>
            </template>
          </el-table-column>
          <el-table-column label="关联房源" min-width="220">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <el-image :src="row.house.coverImage" class="w-12 h-12 rounded-lg object-cover bg-gray-100">
                  <template #error>
                    <div class="w-full h-full flex items-center justify-center bg-gray-100 text-gray-400">
                      <el-icon>
                        <Picture />
                      </el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="flex flex-col">
                  <span class="font-medium text-[#1f1f1f] truncate w-40">{{ row.house.projectName }}</span>
                  <span class="text-xs text-gray-400">{{ row.house.layout }} | {{ row.house.area }}㎡</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="客户信息" min-width="140">
            <template #default="{ row }">
              <div class="flex flex-col">
                <span class="font-medium">{{ row.customer.realName }}</span>
                <span class="text-xs text-gray-400">{{ row.customer.phone }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="成交金额" width="140" align="right">
            <template #default="{ row }">
              <span class="font-bold text-[#1f1f1f]">¥{{ formatMoney(row.dealPrice) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已收金额" width="140" align="right">
            <template #default="{ row }">
              <div class="flex flex-col items-end">
                <span class="text-green-600 font-medium">¥{{ formatMoney(row.actualPaidAmount) }}</span>
                <el-progress :percentage="Math.round((row.actualPaidAmount / row.dealPrice) * 100)" :show-text="false"
                  :stroke-width="2" class="w-16 mt-1" :color="getProgressColor(row.status)" />
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType[row.status]" class="!rounded-full px-4 low-sat-tag" effect="light">
                {{ getStatusLabel(row.status, (row as any).paymentType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="销售" prop="sales.realName" width="100" align="center" />
          <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-hasPermi="['trade:order:query']" link type="primary"
                @click.stop="handlePreview(row)">预览详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="px-6 py-4 border-t border-gray-50 flex justify-end">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :total="total" layout="total, prev, pager, next" background class="google-pagination" />
      </div>
    </div>

    <!-- 新建订单对话框 -->
    <el-dialog v-model="orderDialogVisible" title="新建交易订单" width="900px" class="google-dialog" destroy-on-close>
      <div class="px-4 py-2">
        <el-form :model="orderForm" :rules="computedOrderRules" ref="orderFormRef" label-position="top">
          <el-row :gutter="40">
            <!-- 核心主体 -->
            <el-col :span="12">
              <section class="mb-6">
                <h3
                  class="text-sm font-bold text-[#1a73e8] uppercase tracking-wider mb-4 border-l-4 border-[#1a73e8] pl-3">
                  主体信息</h3>
                <el-form-item label="选择客户" prop="customerId">
                  <el-select v-model="orderForm.customerId" placeholder="搜索/选择客户" class="google-input-flat !w-full"
                    filterable>
                    <el-option v-for="c in customerOptions" :key="c.id" :label="`${c.realName} (${c.phone})`"
                      :value="c.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="关联房源" prop="houseId">
                  <el-select v-model="orderForm.houseId" placeholder="搜索项目/房号" class="google-input-flat !w-full"
                    filterable @change="onHouseChange">
                    <el-option v-for="h in houseOptions" :key="h.id"
                      :label="`${h.projectName} - ${h.houseNo} (${h.area}㎡)`" :value="h.id">
                      <div class="flex items-center justify-between w-full">
                        <span>{{ h.projectName }} - {{ h.houseNo }} ({{ h.area }}㎡)</span>
                        <el-tag :type="h.houseType === 1 ? 'danger' : (h.houseType === 2 ? 'warning' : 'success')"
                          size="small" effect="plain" class="!rounded-full border-none !bg-opacity-10 ml-2">
                          {{ h.houseType === 1 ? '新房' : (h.houseType === 2 ? '二手房' : '租房') }}
                        </el-tag>
                      </div>
                    </el-option>
                  </el-select>
                </el-form-item>
                <el-form-item label="负责销售" prop="salesId">
                  <el-select v-model="orderForm.salesId" :disabled="!canAssignOrder" placeholder="选择销售人员"
                    class="google-input-flat !w-full">
                    <el-option v-for="user in salesOptions" :key="user.id" :label="user.realName" :value="user.id" />
                  </el-select>
                  <div v-if="!canAssignOrder" class="text-[10px] text-gray-400 mt-1 pl-1">
                    注：您无权指派他人，订单将默认关联至您的名下。
                  </div>
                </el-form-item>
              </section>
            </el-col>

            <!-- 财务详情 -->
            <el-col :span="12">
              <section class="mb-6">
                <h3
                  class="text-sm font-bold text-[#1a73e8] uppercase tracking-wider mb-4 border-l-4 border-[#1a73e8] pl-3">
                  价格与财务</h3>

                <RentalOrderForm v-if="isRental" ref="rentalFormRef" :orderForm="orderForm"
                  @rental-data-change="handleRentalData" />
                <SaleOrderForm v-else :orderForm="orderForm" />
              </section>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="orderDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button type="primary" class="!rounded-xl px-8 !bg-[#1a73e8] border-none"
            @click="handleSubmitOrder">生成订单</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情预览抽屉 -->
    <el-drawer v-model="drawerVisible" direction="rtl" size="560px" :with-header="false" class="order-detail-drawer">
      <div v-if="currentOrder" class="h-full flex flex-col bg-white">
        <!-- 抽屉头部 -->
        <div class="p-8 border-b border-gray-50 bg-[#f8f9fa]">
          <div class="flex justify-between items-start mb-6">
            <div>
              <div class="text-xs font-bold text-[#1a73e8] tracking-widest uppercase mb-1">Transaction Details</div>
              <h2 class="text-2xl font-bold text-[#202124]">{{ currentOrder.transactionNo }}</h2>
            </div>
            <el-button icon="Close" circle @click="drawerVisible = false" class="!border-none !bg-white shadow-sm" />
          </div>

          <div class="flex gap-4">
            <div class="flex-1 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
              <div class="text-xs text-gray-400 mb-1">当前状态</div>
              <div class="flex items-center gap-2">
                <span class="w-2 h-2 rounded-full" :class="statusBgColor[currentOrder.status]"></span>
                <span class="font-bold text-[#202124]">{{ getStatusLabel(currentOrder.status, (currentOrder as
                  any).paymentType) }}</span>
              </div>
            </div>
            <div class="flex-1 bg-white p-4 rounded-xl shadow-sm border border-gray-100">
              <div class="text-xs text-gray-400 mb-1">成交总额</div>
              <div class="text-lg font-bold text-[#202124]">¥{{ formatMoney(currentOrder.dealPrice) }}</div>
            </div>
          </div>
        </div>

        <el-scrollbar class="flex-1">
          <div class="p-8 space-y-8">
            <!-- 流程推进（系统自动 + 手动） -->
            <section v-if="currentOrder.status !== 4 && currentOrder.status !== 5">
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]">
                  <MagicStick />
                </el-icon> 流程推进
              </h3>
              <div class="flex flex-wrap gap-3">
                <!-- 定金/首付款/交易完成由支付流水审核通过后自动推进 -->
                <el-tag type="info" effect="plain" class="!rounded-lg !bg-gray-50 !border-dashed">
                  <el-icon class="mr-1">
                    <Clock />
                  </el-icon>
                  {{ currentOrder.paymentType === 4
                    ? (currentOrder.status === 0 ? '待付款（押金到账后自动推进）' :
                      currentOrder.status === 1 ? '待付押金+租金（全部付清后自动完成）' :
                        '待全部账单结清（自动完成）')
                    : (currentOrder.status === 0 ? '待付定金（支付审核通过后自动推进）' :
                      currentOrder.status === 1 ? '待付首付款（支付审核通过后自动推进）' :
                        currentOrder.status === 2 ? '待办理过户' :
                          '待全部账单结清（自动完成）')
                  }}
                </el-tag>

                <el-button v-if="currentOrder.status === 2 && currentOrder.paymentType !== 4"
                  v-hasPermi="['trade:order:edit']" v-dataScope="{ ownerId: currentOrder.sales.id }" type="primary" plain :disabled="isLocked" class="!rounded-xl"
                  @click="updateStatus(3)">办理过户手续</el-button>
                <el-button v-if="currentOrder.status === 0" v-hasPermi="['trade:order:edit']" v-dataScope="{ ownerId: currentOrder.sales.id }" type="danger" plain
                  :disabled="isLocked" class="!rounded-xl" @click="updateStatus(5)">取消订单</el-button>
                <el-button v-if="currentOrder.status === 1 || currentOrder.status === 2"
                  v-hasPermi="['trade:order:edit']" v-dataScope="{ ownerId: currentOrder.sales.id }" type="danger" plain :disabled="isLocked" class="!rounded-xl"
                  @click="handleCancelWithRefund">取消并退款</el-button>
                <div v-if="isLocked" class="w-full text-xs text-red-500 mt-2 flex items-center gap-1">
                  <el-icon>
                    <WarnTriangleFilled />
                  </el-icon> 该订单有流水正在审核中，操作已锁定
                </div>
              </div>
            </section>

            <!-- 财务进度 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]">
                  <Wallet />
                </el-icon> 账单回款计划
              </h3>
              <div class="bg-[#f8f9fa] p-5 rounded-2xl">
                <div class="flex justify-between text-sm mb-2">
                  <span class="text-gray-500">总体进度 {{ Math.round((currentOrder.actualPaidAmount /
                    currentOrder.dealPrice) *
                    100) }}%</span>
                  <span class="font-bold text-[#1a73e8]">¥{{ formatMoney(currentOrder.actualPaidAmount) }} / ¥{{
                    formatMoney(currentOrder.dealPrice) }}</span>
                </div>
                <el-progress
                  :percentage="Math.min(100, Math.round((currentOrder.actualPaidAmount / currentOrder.dealPrice) * 100))"
                  :stroke-width="12" :color="getProgressColor(currentOrder.status)" class="google-progress" />

                <!-- 动态账单列表 -->
                <div class="mt-6 space-y-4">
                  <div v-for="plan in paymentPlans" :key="plan.id"
                    class="p-4 bg-white rounded-xl border border-gray-100 shadow-sm">
                    <div class="flex items-center justify-between mb-3">
                      <div class="flex items-center gap-2">
                        <div class="w-8 h-8 rounded-full flex items-center justify-center"
                          :class="plan.status === 2 ? 'bg-green-50' : 'bg-blue-50'">
                          <el-icon :class="plan.status === 2 ? 'text-green-600' : 'text-blue-600'">
                            <component :is="plan.status === 2 ? 'Check' : 'Clock'" />
                          </el-icon>
                        </div>
                        <div>
                          <span class="text-sm font-bold block">{{ plan.payName }}</span>
                          <span class="text-[10px] text-gray-400">应收日期: {{ plan.dueDate ? plan.dueDate.split('T')[0] :
                            '-'
                          }}</span>
                        </div>
                      </div>
                      <el-tag :type="plan.status === 2 ? 'success' : (plan.status === 1 ? 'warning' : 'info')"
                        size="small" class="!rounded-full px-2">
                        {{ plan.status === 2 ? '已结清' : (plan.status === 1 ? '部分付款' : '待支付') }}
                      </el-tag>
                    </div>
                    <div class="flex justify-between items-end">
                      <div class="text-xs">
                        <div class="text-gray-400 mb-0.5">已收 / 应收</div>
                        <div class="font-mono font-bold">
                          <span class="text-green-600">¥{{ formatMoney(plan.paidAmount) }}</span>
                          <span class="text-gray-300 mx-1">/</span>
                          <span class="text-gray-600">¥{{ formatMoney(plan.receivableAmount) }}</span>
                        </div>
                      </div>
                      <el-button v-if="canSubmitPayment(plan)" v-dataScope="{ ownerId: currentOrder.sales.id }" type="primary" size="small" class="!rounded-lg"
                        @click="handleOpenPaymentSubmit(plan)">录入流水</el-button>
                    </div>
                    <!-- 已提交的支付记录 -->
                    <div v-for="p in planPaymentsMap[plan.id]" :key="p.id"
                      class="flex items-center justify-between mt-3 pt-3 border-t border-gray-50">
                      <div class="flex items-center gap-2 text-xs">
                        <span class="font-mono text-gray-400">{{ p.receiptNo }}</span>
                        <span class="text-gray-500">{{ p.paymentMethod }}</span>
                      </div>
                      <div class="flex items-center gap-2">
                        <span class="font-bold text-sm" :class="p.flowType === 1 ? 'text-green-600' : 'text-red-500'">
                          {{ p.flowType === 1 ? '+' : '-' }}¥{{ formatMoney(p.amount) }}
                        </span>
                        <el-tag v-if="p.paymentStatus === 0" size="small" type="warning"
                          class="!rounded-md">审核中</el-tag>
                        <el-tag v-else-if="p.paymentStatus === 1" size="small" type="success"
                          class="!rounded-md">已通过</el-tag>
                        <el-tag v-else-if="p.paymentStatus === 3" size="small" type="info"
                          class="!rounded-md">已驳回</el-tag>
                        <el-tag v-else size="small" class="!rounded-md">{{ p.paymentStatus === 2 ? '已作废' : '-'
                        }}</el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 房源卡片 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]">
                  <House />
                </el-icon> 房源详情
              </h3>
              <div
                class="flex gap-4 p-4 rounded-2xl border border-gray-100 hover:shadow-md transition-shadow cursor-pointer">
                <el-image :src="currentOrder.house.coverImage" class="w-24 h-24 rounded-xl object-cover shadow-sm">
                  <template #error>
                    <div class="w-full h-full flex items-center justify-center bg-gray-100 text-gray-400">
                      <el-icon :size="32">
                        <Picture />
                      </el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="flex-1">
                  <div class="font-bold text-lg text-[#202124]">{{ currentOrder.house.projectName }}</div>
                  <div class="text-sm text-gray-500 mt-1">{{ currentOrder.house.houseNo }} | {{
                    currentOrder.house.layout }}
                  </div>
                  <div class="flex gap-2 mt-3">
                    <el-tag size="small" type="info" class="!bg-gray-100 !border-none !text-gray-600 !rounded-md">{{
                      currentOrder.house.area }}㎡</el-tag>
                    <el-tag size="small" type="info"
                      class="!bg-gray-100 !border-none !text-gray-600 !rounded-md">朝南</el-tag>
                    <el-tag size="small" type="info"
                      class="!bg-gray-100 !border-none !text-gray-600 !rounded-md">精装修</el-tag>
                  </div>
                </div>
              </div>
            </section>

            <!-- 参与主体 -->
            <section class="grid grid-cols-2 gap-6">
              <div>
                <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                  <el-icon class="text-[#1a73e8]">
                    <User />
                  </el-icon> 客户资料
                </h3>
                <div class="p-4 rounded-2xl bg-blue-50/50 border border-blue-100">
                  <div class="font-bold text-[#1a73e8]">{{ currentOrder.customer.realName }}</div>
                  <div class="text-xs text-blue-400 mt-1">{{ currentOrder.customer.phone }}</div>
                </div>
              </div>
              <div>
                <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                  <el-icon class="text-[#1a73e8]">
                    <UserFilled />
                  </el-icon> 归属销售
                </h3>
                <div class="p-4 rounded-2xl bg-gray-50 border border-gray-100">
                  <div class="font-bold text-gray-700">{{ currentOrder.sales.realName }}</div>
                  <div class="text-xs text-gray-400 mt-1">工号：RES-00{{ currentOrder.sales.id }}</div>
                </div>
              </div>
            </section>

            <!-- 交易时间轴 -->
            <section>
              <h3 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
                <el-icon class="text-[#1a73e8]">
                  <Calendar />
                </el-icon> 交易生命周期
              </h3>
              <el-timeline class="ml-1">
                <el-timeline-item timestamp="2026-05-01 10:00" type="primary" size="large">
                  <span class="font-bold text-gray-700">合同签订</span>
                  <p class="text-xs text-gray-400 mt-1">由销售人员 {{ currentOrder.sales.realName }} 发起创建</p>
                </el-timeline-item>

                <!-- 租房时间线 -->
                <template v-if="currentOrder.paymentType === 4">
                  <el-timeline-item :timestamp="currentOrder.createTime"
                    :type="currentOrder.status >= 1 ? 'success' : 'info'">
                    <span class="font-medium"
                      :class="currentOrder.status >= 1 ? 'text-gray-700' : 'text-gray-400'">押金确认</span>
                  </el-timeline-item>
                  <el-timeline-item v-if="currentOrder.status >= 4" timestamp="2026-05-09" type="success">
                    <span class="font-bold text-green-600">租赁完成</span>
                  </el-timeline-item>
                </template>

                <!-- 买卖时间线 -->
                <template v-else>
                  <el-timeline-item :timestamp="currentOrder.createTime"
                    :type="currentOrder.status >= 1 ? 'success' : 'info'">
                    <span class="font-medium"
                      :class="currentOrder.status >= 1 ? 'text-gray-700' : 'text-gray-400'">定金确认</span>
                  </el-timeline-item>
                  <el-timeline-item v-if="currentOrder.status >= 2" timestamp="2026-05-05" type="success">
                    <span class="font-medium text-gray-700">首付款到账</span>
                  </el-timeline-item>
                  <el-timeline-item v-if="currentOrder.status >= 3" timestamp="2026-05-08" type="success">
                    <span class="font-medium text-gray-700">权属过户</span>
                  </el-timeline-item>
                  <el-timeline-item v-if="currentOrder.status === 4" timestamp="2026-05-09" type="success">
                    <span class="font-bold text-green-600">交易圆满完成</span>
                  </el-timeline-item>
                </template>
              </el-timeline>
            </section>
          </div>
        </el-scrollbar>

        <!-- 底部操作 -->
        <div class="p-6 border-t border-gray-50 flex gap-3">
          <el-button v-hasPermi="['trade:order:print']" class="flex-1 !rounded-xl !h-12" icon="Printer">打印合同</el-button>
          <el-button v-hasPermi="['trade:order:download']" type="primary"
            class="flex-1 !rounded-xl !h-12 !bg-[#1a73e8] border-none" icon="Document">下载电子凭证</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 支付流水提交对话框 -->
    <el-dialog v-model="paymentDialogVisible" title="录入支付流水" width="500px" class="google-dialog" destroy-on-close>
      <div class="p-2">
        <el-alert title="提醒：提交后需等待财务审核核销，审核期间订单状态将被锁定。" type="warning" :closable="false" class="mb-6 !rounded-xl" />
        <el-form :model="paymentForm" :rules="paymentRules" ref="paymentFormRef" label-position="top">
          <el-form-item label="负责销售">
            <el-input :model-value="currentOrder?.sales?.realName" disabled class="google-input-flat !w-full" />
          </el-form-item>
          <div class="grid grid-cols-2 gap-4">
            <el-form-item label="实付金额 (元)" prop="amount">
              <el-input-number v-model="paymentForm.amount" :min="0" :precision="2" :controls="false"
                class="google-input-flat !w-full" />
            </el-form-item>
            <el-form-item label="支付方式" prop="paymentMethod">
              <el-select v-model="paymentForm.paymentMethod" class="google-input-flat !w-full">
                <el-option label="银行转账" value="银行转账" />
                <el-option label="微信支付" value="微信支付" />
                <el-option label="支付宝" value="支付宝" />
                <el-option label="现金" value="现金" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="付款人">
            <el-input v-model="paymentForm.payerInfo" placeholder="客户姓名 / 付款公司名称" class="google-input-flat !w-full" />
          </el-form-item>
          <el-form-item label="支付时间" prop="paymentTime">
            <el-date-picker v-model="paymentForm.paymentTime" type="datetime" placeholder="请选择客户实际支付时间"
              value-format="YYYY-MM-DDTHH:mm:ss" class="google-input-flat !w-full" />
          </el-form-item>
          <el-form-item label="支付凭证 (回单/截图)" prop="proofUrl">
            <el-upload class="google-upload-card" :action="uploadUrl" :headers="headers" :data="{ category: 'PAYMENT' }"
              :on-success="handleUploadSuccess" :show-file-list="false" :limit="1">
              <div v-if="paymentForm.proofUrl" class="relative group w-full h-32">
                <img :src="paymentForm.proofUrl" class="w-full h-full object-cover rounded-xl" />
                <div
                  class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity rounded-xl">
                  <el-icon class="text-white text-2xl">
                    <Edit />
                  </el-icon>
                </div>
              </div>
              <div v-else
                class="w-full h-32 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 rounded-xl hover:border-blue-400 transition-colors bg-gray-50">
                <el-icon class="text-gray-400 text-3xl mb-2">
                  <Plus />
                </el-icon>
                <span class="text-xs text-gray-400">点击上传凭证</span>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注说明" prop="remark">
            <el-input v-model="paymentForm.remark" type="textarea" :rows="3" placeholder="填写补充说明..."
              class="google-input-flat" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex gap-3 justify-end px-4 pb-4">
          <el-button @click="paymentDialogVisible = false" class="!rounded-xl px-6">取消</el-button>
          <el-button type="primary" class="!rounded-xl px-8 !bg-[#1a73e8] border-none"
            @click="handleSubmitPayment">提交审核</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { Search, Refresh, Plus, Download, Close, MagicStick, Check, Clock, Document, Printer, House as HouseIcon, Wallet, User as UserIcon, UserFilled, Calendar, Picture, Delete, Edit, WarnTriangleFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { listOrder, addOrder, updateOrderStatus, getOrder, reqGetPaymentPlansByTransId, cancelOrderWithRefund, exportOrder } from '@/api/trade/order'
import { listCustomer } from '@/api/trade/customer'
import { listHouse } from '@/api/house/house'
import { submitPayment, listPayment } from '@/api/finance/payment'
import { getSalesOptions } from '@/api/system/user'
import RentalOrderForm from './components/RentalOrderForm.vue'
import SaleOrderForm from './components/SaleOrderForm.vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canAssignOrder = computed(() => userStore.permissions.includes('trade:order:assign'))
const currentUserId = computed(() => userStore.userInfo?.id)

// --- 类型定义 ---
interface Transaction {
  id: number
  transactionNo: string
  dealPrice: number
  deposit: number
  downPayment?: number
  loanAmount?: number
  status: number // 0-待付定金, 1-已付定金, 2-已付首付, 3-已过户, 4-已完成, 5-已取消
  actualPaidAmount: number
  createTime: string
  nextPaymentTime?: string
  house: {
    id: number
    houseNo: string
    projectName: string
    layout: string
    area: number
    coverImage: string
  }
  customer: {
    id: number
    realName: string
    phone: string
  }
  sales: {
    id: number
    realName: string
  }
}

// --- 静态映射 ---
const statusMap: Record<number, string> = {
  0: '待付定金',
  1: '已付定金',
  2: '已付首付',
  3: '待过户',
  4: '已完成',
  5: '已取消'
}

const statusTagType: Record<number, string> = {
  0: 'info',
  1: 'primary',
  2: 'success',
  3: 'warning',
  4: 'success',
  5: 'danger'
}

const statusBgColor: Record<number, string> = {
  0: 'bg-gray-400',
  1: 'bg-blue-500',
  2: 'bg-green-400',
  3: 'bg-orange-400',
  4: 'bg-green-600',
  5: 'bg-red-500'
}

// --- 状态与参数 ---
const loading = ref(false)
const total = ref(0)
const orderList = ref<Transaction[]>([])

const currentOrder = ref<Transaction | null>(null)
const drawerVisible = ref(false)
const customerOptions = ref<any[]>([])
const houseOptions = ref<any[]>([])
const salesOptions = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  transactionNo: '',
  customerName: '',
  status: undefined,
  dateRange: []
})

// --- 财务与账单状态 ---
const paymentPlans = ref<any[]>([])
const payments = ref<any[]>([])
const planPaymentsMap = computed(() => {
  const map: Record<number, any[]> = {}
  payments.value.forEach((p: any) => {
    const pid = p.paymentPlanId
    if (!map[pid]) map[pid] = []
    map[pid].push(p)
  })
  return map
})
const isLocked = ref(false)
const selectedPlan = ref<any>(null)
const paymentDialogVisible = ref(false)
const paymentFormRef = ref()
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/v1/common/upload'
const headers = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('resms_token')
}))

const paymentForm = reactive({
  transactionId: 0,
  paymentPlanId: 0,
  paymentType: 1,
  flowType: 1, // 收款
  amount: 0,
  paymentMethod: '银行转账',
  payerInfo: '',
  paymentTime: '',
  proofUrl: '',
  remark: ''
})

const paymentRules = {
  amount: [{ required: true, message: '请输入实付金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
  paymentTime: [{ required: true, message: '请选择支付时间', trigger: 'change' }],
  proofUrl: [{ required: true, message: '请上传支付凭证', trigger: 'change' }]
}

// --- 新建订单状态 ---
const orderDialogVisible = ref(false)
const orderFormRef = ref()
const rentalFormRef = ref()

/** 当前选中房源是否为租房 */
const isRental = computed(() => {
  if (!orderForm.houseId) return false
  const house = houseOptions.value.find(h => h.id === orderForm.houseId)
  return house?.houseType === 3
})

const handleRentalData = (data: { dealPrice: number; deposit: number; paymentPlanList: any[] }) => {
  orderForm.dealPrice = data.dealPrice
  orderForm.deposit = data.deposit
  orderForm.paymentPlanList = data.paymentPlanList
}

const orderForm = reactive({
  customerId: undefined as number | undefined,
  houseId: undefined as number | undefined,
  salesId: 301, // 默认当前用户
  dealPrice: 0,
  deposit: 0,
  downPayment: 0,
  loanAmount: 0,
  paymentType: null as number | null, // 1=一次性, 2=分期, 3=按揭, 4=租房
  paymentPlanList: [] as { payName: string; receivableAmount: number; dueDate: string }[],
  contractDate: '',
  // 租房临时字段（不提交后端，仅用于前端计算）
  monthlyRent: 0,
  depositMonths: 1,
  rentPaymentCycle: 3,
  leaseTerm: 12
})

const computedOrderRules = computed(() => {
  const base = {
    customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
    houseId: [{ required: true, message: '请选择房源', trigger: 'change' }],
    paymentType: [{ required: true, message: '请选择付款方式', trigger: 'change' }]
  }

  if (isRental.value) {
    return {
      ...base,
      monthlyRent: [{ required: true, message: '请输入月租金', trigger: 'blur' }]
    }
  }

  return {
    ...base,
    dealPrice: [{ required: true, message: '请输入成交价', trigger: 'blur' }],
    deposit: [{ required: true, message: '请输入定金', trigger: 'blur' }]
  }
})



// --- 方法 ---
const handleQuery = () => {
  loading.value = true
  listOrder(queryParams).then((res: any) => {
    orderList.value = res.data.records
    total.value = res.data.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

// 初始化加载
handleQuery()

const resetQuery = () => {
  queryParams.transactionNo = ''
  queryParams.customerName = ''
  queryParams.status = undefined
  queryParams.dateRange = []
  handleQuery()
}

const handlePreview = (row: Transaction) => {
  loading.value = true
  Promise.all([
    getOrder(row.id),
    reqGetPaymentPlansByTransId(row.id),
    request({ url: `/system/trade/v1/transactions/${row.id}/lock-status`, method: 'get' }),
    listPayment({ transactionId: row.id, pageSize: 50 })
  ]).then(([orderRes, planRes, lockRes, payRes]: any) => {
    currentOrder.value = orderRes.data
    paymentPlans.value = planRes.data
    isLocked.value = lockRes.data
    payments.value = payRes.data.records || []
    drawerVisible.value = true
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

// --- 支付提交相关 ---
const canSubmitPayment = (plan: any) => {
  if (currentOrder.value?.status === 5) return false // 订单已取消
  if (plan.status === 2 || plan.status === 3) return false // 已结清或已取消
  const payments = planPaymentsMap.value[plan.id]
  if (payments?.some((p: any) => p.paymentStatus === 0)) return false // 有待审核流水
  return true
}

const handleOpenPaymentSubmit = (plan: any) => {
  selectedPlan.value = plan
  paymentForm.transactionId = currentOrder.value!.id
  paymentForm.paymentPlanId = plan.id
  // 根据款项名称自动映射支付类型 (后端 PaymentSubmitForm 的逻辑)
  // 租房：押金→定金类型(1)，租金→其他(5)
  if (plan.payName.includes('押金')) paymentForm.paymentType = 1
  else if (plan.payName.includes('定金')) paymentForm.paymentType = 1
  else if (plan.payName.includes('首付')) paymentForm.paymentType = 2
  else if (plan.payName.includes('贷款') || plan.payName.includes('尾款')) paymentForm.paymentType = 3
  else if (plan.payName.includes('租金')) paymentForm.paymentType = 5
  else paymentForm.paymentType = 5

  paymentForm.amount = plan.receivableAmount - plan.paidAmount
  paymentForm.paymentMethod = '银行转账'
  paymentForm.paymentTime = new Date().toISOString().split('.')[0]
  paymentForm.proofUrl = ''
  paymentForm.payerInfo = ''
  paymentForm.remark = ''
  paymentDialogVisible.value = true
}

const handleUploadSuccess = (res: any) => {
  if (res.code === 200) {
    paymentForm.proofUrl = res.data.url
  }
}

const handleSubmitPayment = () => {
  paymentFormRef.value.validate((valid: boolean) => {
    if (valid) {
      submitPayment(paymentForm).then(() => {
        ElMessage.success('流水提交成功，等待财务审核')
        paymentDialogVisible.value = false
        // 刷新详情及支付记录
        handlePreview(currentOrder.value!)
      })
    }
  })
}

/** 导出数据 */
const handleExport = async () => {
  const params: any = {}
  if (queryParams.transactionNo) params.transactionNo = queryParams.transactionNo
  if (queryParams.customerName) params.customerName = queryParams.customerName
  if (queryParams.status !== undefined) params.status = queryParams.status
  try {
    const res = await exportOrder(queryParams)
    const blob = new Blob([res])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = decodeURIComponent(
      (res as any).headers?.['content-disposition']?.split('filename*=utf-8\'\'')?.[1] || '交易订单.xlsx'
    )
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch {
    // 错误由拦截器处理
  }
}

const handleAdd = async () => {
  orderForm.customerId = undefined
  orderForm.houseId = undefined
  orderForm.dealPrice = 0
  orderForm.deposit = 0
  orderForm.downPayment = 0
  orderForm.loanAmount = 0
  orderForm.paymentType = null
  orderForm.paymentPlanList = []
    // 重置租房字段
    ; (orderForm as any).monthlyRent = 0
    ; (orderForm as any).depositMonths = 1
    ; (orderForm as any).rentPaymentCycle = 3
    ; (orderForm as any).leaseTerm = 12

  // 默认销售：如果没有指派权限，强制设为自己
  if (!canAssignOrder.value) {
    orderForm.salesId = currentUserId.value
  } else {
    orderForm.salesId = currentUserId.value // 即使有权限也默认选自己，方便操作
  }

  // 异步加载下拉数据
  try {
    const [custRes, houseRes, userRes] = await Promise.all([
      listCustomer({ pageSize: 100 }),
      listHouse({ pageSize: 100, status: 1 }),
      getSalesOptions()
    ])
    customerOptions.value = (custRes as any).data.records
    houseOptions.value = (houseRes as any).data.records
    salesOptions.value = (userRes as any).data
    orderDialogVisible.value = true
  } catch (err) {
    ElMessage.error('加载基础数据失败')
  }
}

const onHouseChange = (val: number) => {
  const house = houseOptions.value.find(h => h.id === val)
  if (!house) return

  if (house.houseType === 3) {
    // 租房模式：自动切换到 paymentType=4，按租金字段填默认值
    orderForm.paymentType = 4
    orderForm.monthlyRent = house.price || 0
    orderForm.depositMonths = 1
    orderForm.rentPaymentCycle = 3
    orderForm.leaseTerm = 12
    nextTick(() => rentalFormRef.value?.calcRentalPlans())
  } else {
    // 买卖模式
    orderForm.paymentType = 1
    orderForm.dealPrice = house.price || 0
    orderForm.downPayment = orderForm.dealPrice * 0.3
    orderForm.loanAmount = orderForm.dealPrice * 0.7
    orderForm.paymentPlanList = []
  }

  if (house.salesId) {
    orderForm.salesId = house.salesId
  }
}

const handleSubmitOrder = () => {
  orderFormRef.value.validate((valid: boolean) => {
    if (valid) {
      // 根据模式构造干净的提交数据
      const isRentalForm = isRental.value
      const payload: any = {
        customerId: orderForm.customerId,
        houseId: orderForm.houseId,
        salesId: orderForm.salesId,
        paymentType: orderForm.paymentType
      }

      if (isRentalForm) {
        payload.dealPrice = orderForm.dealPrice
        payload.deposit = 0
        payload.paymentPlanList = orderForm.paymentPlanList
        if (orderForm.nextPaymentTime) payload.nextPaymentTime = orderForm.nextPaymentTime
      } else {
        payload.dealPrice = orderForm.dealPrice
        payload.deposit = orderForm.deposit
        payload.downPayment = orderForm.downPayment
        payload.loanAmount = orderForm.loanAmount
        if (orderForm.paymentType === 2) {
          payload.paymentPlanList = orderForm.paymentPlanList
        }
        if (orderForm.nextPaymentTime) payload.nextPaymentTime = orderForm.nextPaymentTime
      }

      addOrder(payload).then(() => {
        ElMessage.success(isRentalForm ? '租房订单创建成功' : '订单创建成功')
        orderDialogVisible.value = false
        handleQuery()
      })
    }
  })
}

const updateStatus = (newStatus: number) => {
  if (!currentOrder.value) return

  const doUpdate = () => {
    updateOrderStatus({ id: currentOrder.value!.id, status: newStatus }).then(() => {
      ElMessage.success('操作执行成功')
      getOrder(currentOrder.value!.id).then((res: any) => {
        currentOrder.value = res.data
        handleQuery()
      })
    })
  }

  if (newStatus === 5) {
    // status=0 时直接取消，无已入账的支付
    ElMessageBox.confirm('确认取消该交易吗？', '取消确认', { type: 'warning' })
      .then(() => doUpdate()).catch(() => { })
  } else if (newStatus === 3) {
    ElMessageBox.confirm('确认办理过户手续吗？', '操作确认')
      .then(() => doUpdate()).catch(() => { })
  }
}

/** 取消交易并自动退款 */
const handleCancelWithRefund = () => {
  if (!currentOrder.value) return
  ElMessageBox.prompt(
    '该交易已有已入账的收款，取消后将自动创建退款流水。请填写取消原因：',
    '取消并退款',
    {
      confirmButtonText: '确认取消', cancelButtonText: '再想想', type: 'warning', inputPattern: /^.+$/,
      inputErrorMessage: '请填写取消原因'
    }
  ).then(({ value }) => {
    cancelOrderWithRefund({ id: currentOrder.value!.id, reason: value || '交易取消' }).then(() => {
      ElMessage.success('交易已取消，退款流水已提交待审核')
      handleQuery()
      drawerVisible.value = false
    })
  }).catch(() => { })
}

const formatMoney = (val: number) => {
  return val.toLocaleString()
}

const getStatusLabel = (status: number, paymentType?: number) => {
  if (paymentType === 4) {
    const rentalMap: Record<number, string> = { 0: '待付款', 1: '已付押金', 4: '已完成', 5: '已取消' }
    return rentalMap[status] || statusMap[status]
  }
  return statusMap[status]
}

const getProgressColor = (status: number) => {
  if (status === 4) return '#34a853' // 绿色
  if (status === 5) return '#ea4335' // 红色
  if (status === 3) return '#fbbc04' // 黄色
  return '#1a73e8' // 蓝色
}

</script>

<style scoped>
.order-management-container {
  height: calc(100vh - 110px);
}

/* Google 扁平化表格 */
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
  height: 80px;
  cursor: pointer;
}

:deep(.el-table__row td) {
  border-bottom: 1px solid #f1f3f4 !important;
}

/* 输入框扁平化规格 */
:deep(.google-input-flat .el-input__wrapper) {
  border-radius: 12px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.2s;
  padding: 8px 16px !important;
  font-size: 14px;
}

:deep(.google-input-flat .el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px #1a73e8 !important;
}

/* 低饱和度胶囊标签 */
.low-sat-tag {
  border: none;
  font-weight: 600;
}

/* 分页器 */
:deep(.google-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #1a73e8 !important;
}

/* 抽屉样式优化 */
:deep(.order-detail-drawer .el-drawer__body) {
  padding: 0;
}

:deep(.google-progress .el-progress-bar__outer) {
  background-color: #e8eaed !important;
  border-radius: 100px;
}

:deep(.google-progress .el-progress-bar__inner) {
  border-radius: 100px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 隐藏时间线连线 */
:deep(.el-timeline-item__tail) {
  border-left: 2px dashed #e8eaed;
}

:deep(.el-timeline-item__node) {
  background-color: #fff;
  border: 2px solid currentColor;
}

/* 对话框圆角 */
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

/* 步进器扁平化 */
:deep(.el-input-number.google-input-flat .el-input__wrapper) {
  padding-left: 15px !important;
  padding-right: 15px !important;
}
</style>
