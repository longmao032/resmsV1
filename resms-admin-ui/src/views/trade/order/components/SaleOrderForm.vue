<template>
  <el-form-item label="成交总价 (元)" prop="dealPrice">
    <el-input-number v-model="orderForm.dealPrice" :min="0" :precision="2" :controls="false"
      class="google-input-flat !w-full text-left" placeholder="0.00" />
  </el-form-item>

  <el-form-item label="付款方式" prop="paymentType">
    <el-select v-model="orderForm.paymentType" class="google-input-flat !w-full" placeholder="请选择付款方式">
      <el-option label="一次性付款" :value="1" />
      <el-option label="分期付款" :value="2" />
      <el-option label="按揭贷款" :value="3" />
    </el-select>
  </el-form-item>

  <div class="grid grid-cols-2 gap-4">
    <el-form-item label="定金金额 (元)" prop="deposit">
      <el-input-number v-model="orderForm.deposit" :min="0" :precision="2" :controls="false"
        class="google-input-flat !w-full" />
    </el-form-item>
    <el-form-item v-if="orderForm.paymentType === 3" label="首付款 (元)" prop="downPayment">
      <el-input-number v-model="orderForm.downPayment" :min="0" :precision="2" :controls="false"
        class="google-input-flat !w-full" />
    </el-form-item>
  </div>

  <el-form-item v-if="orderForm.paymentType === 1" label="尾款金额 (元)">
    <el-input-number v-model="orderForm.loanAmount" :precision="2" :controls="false"
      class="google-input-flat !w-full" disabled />
  </el-form-item>

  <el-form-item v-if="orderForm.paymentType === 3" label="贷款金额 (元)">
    <el-input-number v-model="orderForm.loanAmount" :precision="2" :controls="false"
      class="google-input-flat !w-full" disabled />
  </el-form-item>

  <!-- 分期付款：动态表格 -->
  <div v-if="orderForm.paymentType === 2"
    class="mt-4 border border-gray-100 rounded-2xl p-6 bg-gray-50/50 shadow-inner">
    <div class="flex justify-between items-center mb-4">
      <span class="text-base font-bold text-gray-800">分期计划明细</span>
      <el-button type="primary" link icon="Plus" @click="addPaymentPlan" class="!text-sm">添加期数</el-button>
    </div>
    <div v-for="(plan, index) in orderForm.paymentPlanList" :key="index"
      class="mb-4 p-4 bg-white rounded-xl border border-gray-50 shadow-sm relative">
      <div class="flex gap-3 mb-3 items-center">
        <el-input v-model="plan.payName" placeholder="期数" class="google-input-flat !w-20" />
        <el-input-number v-model="plan.receivableAmount" :min="0" :precision="2" :controls="false"
          placeholder="应收金额" class="google-input-flat flex-1" />
        <el-button type="danger" link icon="Delete" @click="removePaymentPlan(index)" />
      </div>
      <el-date-picker v-model="plan.dueDate" type="datetime" placeholder="请选择该笔款项应收日期"
        value-format="YYYY-MM-DDTHH:mm:ss" class="google-input-flat !w-full" />
    </div>
    <div class="mt-4 p-4 rounded-xl flex justify-between items-center text-sm shadow-sm"
      :class="isPlanSumValid ? 'bg-green-50 text-green-700 border border-green-100' : 'bg-red-50 text-red-600 border border-red-100'">
      <div class="flex flex-col">
        <span class="text-xs opacity-70">当前已排汇总(含定金)</span>
        <span class="font-bold text-base">¥{{ formatMoney(planSum + orderForm.deposit) }}</span>
      </div>
      <div class="text-right">
        <span class="font-bold block">{{ isPlanSumValid ? '金额校验通过' : '金额不匹配' }}</span>
        <span class="text-xs opacity-70">目标总价: ¥{{ formatMoney(orderForm.dealPrice) }}</span>
      </div>
    </div>
  </div>

  <div v-if="orderForm.paymentType === 3" class="p-4 bg-blue-50 rounded-xl mt-2 border border-blue-100">
    <div class="flex justify-between text-xs text-blue-600 mb-1">
      <span>首付比例</span>
      <span class="font-bold">{{ downPaymentRatio }}%</span>
    </div>
    <el-progress :percentage="downPaymentRatio" :show-text="false" :stroke-width="4" color="#1a73e8" />
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'

const props = defineProps<{
  orderForm: any
}>()

const formatMoney = (val: number) => {
  return val.toLocaleString()
}

const addPaymentPlan = () => {
  props.orderForm.paymentPlanList.push({
    payName: `第${props.orderForm.paymentPlanList.length + 1}期`,
    receivableAmount: 0,
    dueDate: ''
  })
}

const removePaymentPlan = (index: number) => {
  props.orderForm.paymentPlanList.splice(index, 1)
}

const planSum = computed(() => {
  return props.orderForm.paymentPlanList.reduce((sum: number, item: any) => sum + (item.receivableAmount || 0), 0)
})

const isPlanSumValid = computed(() => {
  if (props.orderForm.paymentType !== 2) return true
  return Math.abs(props.orderForm.dealPrice - props.orderForm.deposit - planSum.value) < 0.01
})

const downPaymentRatio = computed(() => {
  if (!props.orderForm.dealPrice) return 0
  return Math.round((props.orderForm.downPayment / props.orderForm.dealPrice) * 100)
})

// 自动计算尾款或贷款金额
watch(() => [props.orderForm.dealPrice, props.orderForm.deposit, props.orderForm.downPayment, props.orderForm.paymentType], () => {
  if (props.orderForm.paymentType === 1) {
    props.orderForm.loanAmount = Math.max(0, props.orderForm.dealPrice - props.orderForm.deposit)
  } else if (props.orderForm.paymentType === 3) {
    props.orderForm.loanAmount = Math.max(0, props.orderForm.dealPrice - props.orderForm.downPayment)
  }
}, { deep: true })
</script>
