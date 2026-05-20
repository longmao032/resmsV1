<template>
  <el-form-item label="月租金 (元/月)" prop="monthlyRent">
    <el-input-number v-model="orderForm.monthlyRent" :min="0" :precision="2" :controls="false"
      class="google-input-flat !w-full" placeholder="0.00" />
  </el-form-item>
  <div class="grid grid-cols-3 gap-4">
    <el-form-item label="押金方式">
      <el-select v-model="orderForm.depositMonths" class="google-input-flat !w-full">
        <el-option label="押一" :value="1" />
        <el-option label="押二" :value="2" />
        <el-option label="押三" :value="3" />
      </el-select>
    </el-form-item>
    <el-form-item label="付租周期">
      <el-select v-model="orderForm.rentPaymentCycle" class="google-input-flat !w-full">
        <el-option label="月付（每月）" :value="1" />
        <el-option label="季付（每3个月）" :value="3" />
        <el-option label="半年付" :value="6" />
        <el-option label="年付" :value="12" />
      </el-select>
    </el-form-item>
    <el-form-item label="租期 (月)">
      <el-input-number v-model="orderForm.leaseTerm" :min="1" :max="120"
        class="!w-full google-input-flat" :controls="false" />
    </el-form-item>
  </div>

  <!-- 生成的租金计划 -->
  <div class="mt-4 border border-green-100 rounded-2xl p-4 bg-green-50/30">
    <div class="flex justify-between items-center mb-3">
      <span class="text-sm font-bold text-gray-700">费用汇总</span>
      <span class="text-xs text-gray-400">押金 ¥{{ formatMoney(orderForm.monthlyRent * orderForm.depositMonths) }} + 总租金 ({{ orderForm.leaseTerm }}个月)</span>
    </div>
    <div v-for="(plan, i) in localPlans" :key="i"
      class="flex items-center justify-between py-2 text-sm border-b border-green-100/50 last:border-none">
      <span class="font-medium">{{ plan.payName }}</span>
      <span class="font-bold text-green-700">¥{{ formatMoney(plan.receivableAmount) }}</span>
    </div>
    <div class="flex justify-between pt-3 mt-2 border-t border-green-200 text-sm font-bold">
      <span>合计</span>
      <span>¥{{ formatMoney(localDealPrice) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  orderForm: any
}>()

const emit = defineEmits<{
  'rental-data-change': [data: { dealPrice: number; deposit: number; paymentPlanList: any[] }]
}>()

const localPlans = ref<any[]>([])
const localDealPrice = ref(0)

const formatMoney = (val: number) => {
  return val.toLocaleString()
}

const calcRentalPlans = () => {
  const r = props.orderForm
  if (!r.monthlyRent || !r.leaseTerm) return

  const depositAmount = r.monthlyRent * (r.depositMonths || 1)
  const totalRent = r.monthlyRent * r.leaseTerm
  const cycle = r.rentPaymentCycle || 1
  const periods = Math.ceil(r.leaseTerm / cycle)
  const now = new Date()
  const fmtDateTime = (d: Date) => {
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }

  const deposit = 0
  const dealPrice = totalRent + depositAmount

  const plans: { payName: string; receivableAmount: number; dueDate: string }[] = []
  plans.push({ payName: '押金', receivableAmount: depositAmount, dueDate: fmtDateTime(now) })

  let remainingMonths = r.leaseTerm
  for (let i = 1; i <= periods; i++) {
    const monthsInThisPeriod = Math.min(cycle, remainingMonths)
    const d = new Date(now)
    d.setMonth(d.getMonth() + i * cycle)
    plans.push({
      payName: `第${i}期租金`,
      receivableAmount: r.monthlyRent * monthsInThisPeriod,
      dueDate: fmtDateTime(d)
    })
    remainingMonths -= monthsInThisPeriod
  }

  localPlans.value = plans
  localDealPrice.value = dealPrice

  emit('rental-data-change', { dealPrice, deposit, paymentPlanList: plans })
}

// 自动监听租房字段变化并重新计算
watch(
  () => [props.orderForm.monthlyRent, props.orderForm.depositMonths, props.orderForm.rentPaymentCycle, props.orderForm.leaseTerm],
  () => calcRentalPlans(),
  { immediate: false }
)

defineExpose({ calcRentalPlans })
</script>
