<template>
  <div class="house-management-container p-6 bg-gray-50/50 min-h-full">
    <!-- 1. 顶部统计看板 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      <template v-if="statsLoading">
        <el-card v-for="i in 4" :key="i" shadow="never"
          class="!border-none rounded-[24px] shadow-sm overflow-hidden">
          <div class="p-5 animate-pulse">
            <div class="flex items-center justify-between mb-2">
              <div class="h-3 w-16 bg-gray-200 rounded-full"></div>
              <div class="h-9 w-9 bg-gray-200 rounded-xl"></div>
            </div>
            <div class="h-7 w-24 bg-gray-200 rounded-lg mb-2"></div>
            <div class="h-3 w-20 bg-gray-100 rounded-full"></div>
          </div>
        </el-card>
      </template>
      <el-card v-for="stat in stats" :key="stat.title" shadow="never"
        class="!border-none rounded-[24px] shadow-sm overflow-hidden relative group">
        <div class="p-5">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm text-gray-500 font-medium">{{ stat.title }}</span>
            <el-icon :class="stat.iconColor" class="bg-gray-50 p-2 rounded-xl" :size="20">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <div class="text-2xl font-bold text-gray-900 mb-1">{{ stat.value }}</div>
          <div class="text-[10px]" :class="stat.trend > 0 ? 'text-green-500' : 'text-red-500'">
            <el-icon>
              <CaretTop v-if="stat.trend > 0" />
              <CaretBottom v-else />
            </el-icon>
            {{ Math.abs(stat.trend) }}% 较上月
          </div>
        </div>
      </el-card>
    </div>

    <!-- 2. 搜索区域 -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm mb-6">
      <el-form :inline="true" :model="queryParams" class="search-form -mb-4">
        <el-form-item label="房源编号">
          <el-input v-model="queryParams.houseNo" placeholder="编号" clearable class="google-input !w-32"
            @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="房号">
          <el-input v-model="queryParams.roomNo" placeholder="室号" clearable class="google-input !w-32"
            @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.houseType" placeholder="全部类型" clearable class="google-input !w-32">
            <el-option label="新房" :value="1" />
            <el-option label="二手房" :value="2" />
            <el-option label="租房" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="google-input !w-32">
            <el-option label="待审核" :value="0" />
            <el-option label="在售" :value="1" />
            <el-option label="已预订" :value="2" />
            <el-option label="已成交" :value="3" />
            <el-option label="下架" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="queryParams.city" placeholder="城市" clearable class="google-input !w-32"
            @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button v-hasPermi="['house:house:query']" type="primary" icon="Search" class="!rounded-full px-5"
            @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" class="!rounded-full px-5" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 3. 数据列表 -->
    <el-card shadow="never" class="!border-none rounded-[24px] shadow-sm">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex gap-3">
            <el-button v-hasPermi="['house:house:save']" type="primary" icon="Plus" class="!rounded-full px-5"
              @click="handleAdd">新增房源</el-button>
            <el-button v-hasPermi="['house:house:export']" icon="Download" class="!rounded-full px-5" plain
              @click="handleExport">导出房源</el-button>
          </div>
          <el-radio-group v-model="viewMode" size="small" class="google-radio-group">
            <el-radio-button value="list"><el-icon>
                <List />
              </el-icon></el-radio-button>
            <el-radio-button value="grid"><el-icon>
                <Grid />
              </el-icon></el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table v-if="viewMode === 'list'" v-loading="loading" :data="houseList" class="house-table">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="房源基本信息" min-width="320">
          <template #default="{ row }">
            <div class="flex items-center gap-4">
              <div class="relative group cursor-pointer overflow-hidden rounded-2xl shadow-sm border border-gray-100"
                @click="handleView(row)">
                <el-image :src="row.coverUrl"
                  class="w-28 h-20 object-cover transition-transform duration-500 group-hover:scale-110">
                  <template #error>
                    <div class="w-full h-full bg-gray-50 flex flex-col items-center justify-center text-gray-300">
                      <el-icon :size="24">
                        <Picture />
                      </el-icon>
                      <span class="text-[10px] mt-1 font-bold">无封面</span>
                    </div>
                  </template>
                </el-image>
                <div class="absolute inset-0 bg-black/5 group-hover:bg-black/0 transition-colors" />
                <el-tag :type="getHouseTypeTag(row.houseType)" size="small" effect="dark"
                  class="absolute top-1 left-1 !border-none !rounded-lg scale-90 origin-top-left">
                  {{ getHouseTypeText(row.houseType) }}
                </el-tag>
              </div>
              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-2 mb-1.5">
                  <span
                    class="font-bold text-[#1f1f1f] text-base truncate group-hover:text-blue-600 transition-colors cursor-pointer">{{
                      row.projectName }}</span>
                  <el-tag v-if="row.isHot" type="danger" size="small"
                    class="!rounded-md !text-[10px] !bg-red-50 !text-red-500 !border-red-100">HOT</el-tag>
                </div>
                <div class="text-[12px] text-gray-500 mb-2 flex items-center gap-1">
                  <el-icon class="text-blue-400">
                    <Location />
                  </el-icon>
                  {{ row.district }} · {{ row.buildingNo }}号楼{{ row.roomNo }}室
                  <span class="mx-1 text-gray-300">|</span>
                  <span class="text-gray-400 font-mono">{{ row.houseNo }}</span>
                </div>
                <div class="flex flex-wrap gap-1.5">
                  <el-tag v-for="(tag, index) in row.tags" :key="tag" size="small"
                    :class="[getTagColorClass(index), '!border-none !text-[10px] !rounded-lg px-2']">
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="规格参数" width="200" align="left">
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <div class="flex items-center gap-1.5">
                <span class="text-gray-900 font-bold">{{ row.layout }}</span>
                <span class="text-xs text-gray-400">/</span>
                <span class="text-gray-600">{{ row.area }}㎡</span>
              </div>
              <div class="flex items-center gap-2 text-[11px] text-gray-500">
                <span class="bg-gray-100 px-1.5 py-0.5 rounded-md">{{ row.orientation }}向</span>
                <span class="bg-blue-50 text-blue-600 px-1.5 py-0.5 rounded-md">{{ row.floor }}/{{ row.totalFloor
                }}层</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格信息" width="160" align="right">
          <template #default="{ row }">
            <div class="flex flex-col items-end leading-tight">
              <div class="flex items-baseline gap-0.5">
                <span class="text-red-500 font-black text-2xl tracking-tighter">{{ row.price }}</span>
                <span class="text-red-400 text-xs font-bold">{{ getPriceUnitText(row.priceUnit) }}</span>
              </div>
              <div class="text-[11px] text-gray-400 mt-1.5 flex items-center gap-1">
                <el-icon :size="10">
                  <Histogram />
                </el-icon>
                约 {{ row.unitPrice }} 元/㎡
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责销售" width="130" align="center">
          <template #default="{ row }">
            <div class="flex flex-col items-center gap-1.5 group cursor-pointer">
              <el-avatar v-if="row.salesName" :size="28"
                class="!bg-blue-600 text-white text-[12px] font-bold shadow-md group-hover:scale-110 transition-transform">
                {{ row.salesName.substring(0, 1) }}
              </el-avatar>
              <span class="text-[11px] text-gray-700 font-semibold group-hover:text-blue-600">{{ row.salesName || '未指派'
              }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="当前状态" width="140" align="center">
          <template #default="{ row }">
            <div class="flex flex-col items-center gap-1.5">
              <el-tag :type="getStatusTag(row.status)"
                class="!rounded-full px-4 font-bold"
                effect="light">
                {{ getStatusText(row.status) }}
              </el-tag>
              <div class="flex items-center gap-1 text-[10px] text-gray-400">
                <el-icon :size="10">
                  <Clock />
                </el-icon>
                {{ row.updateTime ? row.updateTime.substring(0, 10) : '-' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex gap-1 justify-center">
              <el-tooltip v-if="row.status === 0" content="房源审核" placement="top" :show-arrow="false">
                <el-button v-hasPermi="['house:house:audit']" link icon="Stamp"
                  class="!p-2.5 hover:bg-orange-50 rounded-full transition-all text-orange-600"
                  @click="handleStatusAudit(row)" />
              </el-tooltip>
              <el-tooltip content="查看详情" placement="top" :show-arrow="false">
                <el-button v-hasPermi="['house:house:query']" link icon="View"
                  class="!p-2.5 hover:bg-green-50 rounded-full transition-all text-green-600"
                  @click="handleView(row)" />
              </el-tooltip>
              <el-tooltip content="编辑房源" placement="top" :show-arrow="false">
                <el-button v-if="row.status !== 2 && row.status !== 3" v-hasPermi="['house:house:save']" v-dataScope="{ ownerId: row.salesId }" link icon="Edit"
                  class="!p-2.5 hover:bg-blue-50 rounded-full transition-all text-blue-600 hover:rotate-12"
                  @click="handleUpdate(row)" />
              </el-tooltip>
              <el-tooltip content="删除房源" placement="top" :show-arrow="false">
                <el-button v-if="row.status !== 2 && row.status !== 3" v-hasPermi="['house:house:delete']" v-dataScope="{ ownerId: row.salesId }" link icon="Delete"
                  class="!p-2.5 hover:bg-red-50 rounded-full transition-all text-red-500 hover:scale-110"
                  @click="handleDelete(row)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 网格模式 -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 p-2">
        <div v-for="h in houseList" :key="h.id"
          class="group relative bg-white rounded-[28px] overflow-hidden shadow-sm hover:shadow-xl transition-all duration-500 border border-transparent hover:border-blue-100">
          <div class="relative aspect-[4/3] overflow-hidden">
            <el-image :src="h.coverUrl"
              class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110">
              <template #error>
                <div class="w-full h-full bg-gray-50 flex flex-col items-center justify-center text-gray-300">
                  <el-icon :size="32">
                    <Picture />
                  </el-icon>
                  <span class="text-xs mt-2 font-bold">暂无图片</span>
                </div>
              </template>
            </el-image>
            <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/20" />
            <div class="absolute top-4 left-4 flex gap-2">
              <el-tag size="small" effect="dark"
                class="!rounded-lg !border-none !bg-white/20 !backdrop-blur-md !text-white px-2">
                {{ getHouseTypeText(h.houseType) }}
              </el-tag>
            </div>
            <div class="absolute top-4 right-4">
              <el-tag :type="getStatusTag(h.status)" size="small" effect="dark"
                class="!rounded-lg !border-none shadow-lg px-3 font-bold">
                {{ getStatusText(h.status) }}
              </el-tag>
            </div>
            <div class="absolute bottom-4 left-4">
              <div class="flex items-baseline text-white">
                <span class="text-2xl font-black">{{ h.price }}</span>
                <span class="text-xs ml-1 font-bold">{{ getPriceUnitText(h.priceUnit) }}</span>
              </div>
            </div>
          </div>
          <div class="p-5">
            <div class="flex justify-between items-start mb-2">
              <h3 class="font-black text-gray-900 text-lg truncate flex-1">{{ h.projectName }}</h3>
              <el-icon class="text-gray-300 hover:text-blue-500 cursor-pointer transition-colors">
                <MoreFilled />
              </el-icon>
            </div>
            <div class="flex items-center gap-2 text-xs text-gray-500 mb-4">
              <el-icon>
                <Location />
              </el-icon>
              <span>{{ h.district }}</span>
              <span class="w-1 h-1 bg-gray-300 rounded-full" />
              <span>{{ h.area }}㎡</span>
              <span class="w-1 h-1 bg-gray-300 rounded-full" />
              <span>{{ h.layout }}</span>
            </div>
            <div class="flex flex-wrap gap-1.5 mb-5">
              <el-tag v-for="(tag, idx) in h.tags.slice(0, 3)" :key="tag" size="small"
                :class="[getTagColorClass(idx), '!border-none !text-[10px] !rounded-lg px-2']">
                {{ tag }}
              </el-tag>
            </div>
            <div class="flex items-center justify-between pt-4 border-t border-gray-50">
              <div class="flex items-center gap-2">
                <el-avatar v-if="h.salesName" :size="24" class="!bg-blue-50 text-blue-600 text-[10px] font-black">{{
                  h.salesName.substring(0, 1) }}</el-avatar>
                <span class="text-[11px] text-gray-600 font-medium">{{ h.salesName || '未指派' }}</span>
              </div>
              <div
                class="flex gap-2 translate-y-1 opacity-0 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300">
                <el-button v-hasPermi="['house:house:query']" circle icon="View" size="small"
                  class="!border-none !bg-green-50 !text-green-600" @click="handleView(h)" />
                <el-button v-if="h.status !== 2 && h.status !== 3" v-hasPermi="['house:house:save']" v-dataScope="{ ownerId: h.salesId }" circle icon="Edit" size="small"
                  class="!border-none !bg-blue-50 !text-blue-600" @click="handleUpdate(h)" />
                <el-button v-if="h.status !== 2 && h.status !== 3" v-hasPermi="['house:house:delete']" v-dataScope="{ ownerId: h.salesId }" circle icon="Delete" size="small"
                  class="!border-none !bg-red-50 !text-red-600" @click="handleDelete(h)" />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex justify-end mt-6">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" background
          layout="total, sizes, prev, pager, next" :total="total" class="google-pagination" @size-change="handleQuery"
          @current-change="handleQuery" />
      </div>
    </el-card>

    <!-- 4. 新增与编辑抽屉 -->
    <el-drawer v-model="drawerVisible" :title="dialogTitle" size="850px" append-to-body class="google-drawer">
      <div class="px-6 pb-60">
        <el-form ref="houseRef" :model="houseForm" :rules="houseRules" label-position="top" class="house-form-drawer"
          :disabled="isReadOnly">
          <el-tabs v-model="activeTab" class="house-tabs">
            <el-tab-pane label="基本参数" name="basic">
              <div class="mt-4 space-y-6">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="所属项目" prop="projectId">
                      <el-select v-model="houseForm.projectId" placeholder="请选择所属项目" class="w-full google-input">
                        <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="负责销售">
                      <el-select v-model="houseForm.salesId" :disabled="isReadOnly || !canAssignSales"
                        placeholder="指派销售人员" class="w-full google-input">
                        <el-option v-for="user in salesOptions" :key="user.id" :label="user.name" :value="user.id">
                          <div class="flex items-center gap-2">
                            <el-avatar :size="20" class="!bg-blue-50 text-blue-600 text-[10px] font-bold">{{
                              user.name.substring(0, 1) }}</el-avatar>
                            <span>{{ user.name }}</span>
                          </div>
                        </el-option>
                      </el-select>
                      <div v-if="!canAssignSales && !isReadOnly" class="text-[10px] text-gray-400 mt-1 pl-1">
                        您无权指派其他销售，房源将默认归属于您
                      </div>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="6">
                    <el-form-item label="楼栋号">
                      <el-input v-model="houseForm.buildingNo" placeholder="如: 8" class="google-input" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="单元号">
                      <el-input v-model="houseForm.unitNo" placeholder="如: 2" class="google-input" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="房号" prop="roomNo">
                      <el-input v-model="houseForm.roomNo" placeholder="如: 1202" class="google-input" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="6">
                    <el-form-item label="房源类型" prop="houseType">
                      <el-select v-model="houseForm.houseType" class="w-full google-input">
                        <el-option label="新房" :value="1" />
                        <el-option label="二手房" :value="2" />
                        <el-option label="租房" :value="3" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item label="户型规格" prop="layout">
                      <el-input v-model="houseForm.layout" placeholder="如: 3室2厅2卫" class="google-input" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="所在楼层" prop="floor">
                      <el-input-number v-model="houseForm.floor" class="!w-full" controls-position="right"
                        placeholder="12" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="总楼层" prop="totalFloor">
                      <el-input-number v-model="houseForm.totalFloor" class="!w-full" controls-position="right"
                        placeholder="28" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item label="朝向">
                      <el-select v-model="houseForm.orientation" class="w-full google-input">
                        <el-option label="南" value="南" />
                        <el-option label="北" value="北" />
                        <el-option label="东" value="东" />
                        <el-option label="西" value="西" />
                        <el-option label="南北" value="南北" />
                        <el-option label="东西" value="东西" />
                        <el-option label="东南" value="东南" />
                        <el-option label="西南" value="西南" />
                        <el-option label="东北" value="东北" />
                        <el-option label="西北" value="西北" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="装修情况">
                      <el-select v-model="houseForm.decoration" class="w-full google-input">
                        <el-option label="毛坯" value="毛坯" />
                        <el-option label="简装" value="简装" />
                        <el-option label="精装" value="精装" />
                        <el-option label="豪装" value="豪装" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="建筑面积 (㎡)" prop="area">
                      <el-input-number v-model="houseForm.area" :precision="2" class="!w-full"
                        controls-position="right" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="24">
                    <el-form-item label="房源标签">
                      <el-select v-model="houseForm.tags" multiple filterable allow-create default-first-option
                        placeholder="请输入或选择标签" class="w-full google-input-tags">
                        <el-option v-for="item in houseTags" :key="item" :label="item" :value="item" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="售价/租金" prop="price">
                      <el-input-number v-model="houseForm.price" class="!w-full" controls-position="right">
                        <template #suffix>{{ priceSuffix }}</template>
                      </el-input-number>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="价格单位" prop="priceUnit">
                      <el-select v-model="houseForm.priceUnit" class="google-input w-full">
                        <el-option label="元/㎡" :value="1" />
                        <el-option label="万元" :value="2" />
                        <el-option label="元/月" :value="3" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <div class="bg-gray-50/50 p-4 rounded-3xl border-2 border-dashed border-gray-100 group transition-all"
                  :class="isReadOnly ? 'cursor-default' : 'cursor-pointer hover:bg-blue-50/20'"
                  @click="!isReadOnly && handlePickMap()">
                  <div class="flex items-center justify-between mb-2">
                    <span class="text-sm font-bold text-gray-700 flex items-center">
                      <el-icon class="mr-1 text-blue-500">
                        <LocationFilled />
                      </el-icon> 地理坐标拾取
                    </span>
                    <el-tag v-if="houseForm.coordinate.lng" type="success" size="small" effect="dark"
                      class="!rounded-md">已定位</el-tag>
                    <el-tag v-else type="info" size="small" class="!rounded-md">未设置</el-tag>
                  </div>
                  <div
                    class="h-32 bg-gray-200/50 rounded-2xl flex items-center justify-center relative overflow-hidden">
                    <div v-if="houseForm.coordinate.lng" class="text-center z-10">
                      <div class="text-blue-600 font-bold text-lg">经纬度已采集</div>
                      <div class="text-[10px] text-gray-500">LNG: {{ houseForm.coordinate.lng }} | LAT: {{
                        houseForm.coordinate.lat }}</div>
                    </div>
                    <span v-else class="text-gray-400 text-xs">点击开启交互式地图选择器</span>
                  </div>
                </div>

                <!-- 动态扩展字段 -->
                <div class="business-extension mt-6">
                  <el-divider content-position="left"><span
                      class="text-xs text-gray-400 font-normal">业务扩展字段</span></el-divider>

                  <!-- 1. 新房扩展 (Type 1) -->
                  <div v-if="houseForm.houseType === 1"
                    class="bg-red-50/20 p-6 rounded-[32px] border border-red-100/50">
                    <el-row :gutter="24">
                      <el-col :span="12">
                        <el-form-item label="预售许可证号">
                          <el-input v-model="houseForm.extend.preSaleLicenseNo" placeholder="请输入预售证号"
                            class="google-input" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="预计交房日期">
                          <el-date-picker v-model="houseForm.extend.estimatedDeliveryDate" type="date"
                            value-format="YYYY-MM-DD" placeholder="选择日期" class="!w-full google-input" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="8">
                        <el-form-item label="备案价 (元/㎡)">
                          <el-input-number v-model="houseForm.extend.recordPrice" class="!w-full"
                            controls-position="right" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="楼盘均价 (元/㎡)">
                          <el-input-number v-model="houseForm.extend.avgPrice" class="!w-full"
                            controls-position="right" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="产权年限 (年)">
                          <el-input-number v-model="houseForm.extend.propertyRightYears" :min="1" :max="100"
                            class="!w-full" controls-position="right" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="8">
                        <el-form-item label="交付标准">
                          <el-select v-model="houseForm.extend.deliveryStandard" class="w-full google-input">
                            <el-option label="毛坯" value="毛坯" />
                            <el-option label="简装" value="简装" />
                            <el-option label="精装" value="精装" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="梯户比">
                          <el-input v-model="houseForm.extend.elevatorRatio" placeholder="如: 2梯4户"
                            class="google-input" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="得房率 (%)">
                          <el-input-number v-model="houseForm.extend.actualAreaRate" :precision="2" :min="0" :max="100"
                            class="!w-full" controls-position="right" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </div>

                  <!-- 2. 二手房扩展 (Type 2) -->
                  <div v-else-if="houseForm.houseType === 2"
                    class="bg-orange-50/20 p-6 rounded-[32px] border border-orange-100/50">
                    <el-row :gutter="24">
                      <el-col :span="12">
                        <el-form-item label="总价 (万元)">
                          <el-input-number v-model="houseForm.extend.totalPrice" class="!w-full"
                            controls-position="right" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="建筑年代">
                          <el-date-picker v-model="houseForm.extend.buildYear" type="year" value-format="YYYY"
                            placeholder="选择年份" class="!w-full google-input" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="12">
                        <el-form-item label="房屋用途">
                          <el-input v-model="houseForm.extend.houseUsage" placeholder="如: 普通住宅" class="google-input" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="上次交易时间">
                          <el-date-picker v-model="houseForm.extend.lastTransactionTime" type="datetime"
                            value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择日期时间" class="!w-full google-input" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="6">
                        <el-form-item label="唯一住房">
                          <el-radio-group v-model="houseForm.extend.isOnlyHouse" class="google-radio-group-alt">
                            <el-radio-button :value="1">是</el-radio-button>
                            <el-radio-button :value="0">否</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                      </el-col>
                      <el-col :span="6">
                        <el-form-item label="是否满二">
                          <el-radio-group v-model="houseForm.extend.isFullTwo" class="google-radio-group-alt">
                            <el-radio-button :value="1">是</el-radio-button>
                            <el-radio-button :value="0">否</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                      </el-col>
                      <el-col :span="6">
                        <el-form-item label="是否满五">
                          <el-radio-group v-model="houseForm.extend.isFullFive" class="google-radio-group-alt">
                            <el-radio-button :value="1">是</el-radio-button>
                            <el-radio-button :value="0">否</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                      </el-col>
                      <el-col :span="6">
                        <el-form-item label="抵押状态">
                          <el-radio-group v-model="houseForm.extend.mortgageStatus" class="google-radio-group-alt">
                            <el-radio-button :value="1">有</el-radio-button>
                            <el-radio-button :value="0">无</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="16">
                        <el-form-item label="房本详情信息">
                          <el-input v-model="houseForm.extend.propertyDeed" type="textarea" :rows="5"
                            placeholder="请输入房本详情，如编号、权属人等" class="google-input !rounded-2xl" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="房本照片附件">
                          <div class="flex items-center gap-3 bg-gray-50/50 p-2 rounded-xl border border-gray-100">
                            <template v-if="houseForm.extend.propertyDeedUrl">
                              <div class="flex items-center gap-1 text-blue-600 hover:text-blue-500 cursor-pointer transition-colors"
                                @click="handlePictureCardPreview({ url: houseForm.extend.propertyDeedUrl })">
                                <el-icon><View /></el-icon>
                                <span class="text-sm">预览照片</span>
                              </div>
                              <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file"
                                :data="{ category: 'DEED' }" :show-file-list="false"
                                :on-success="handleDeedUploadSuccess" :disabled="isReadOnly" class="inline-block">
                                <el-button v-if="!isReadOnly" link type="warning" icon="Edit">更换</el-button>
                              </el-upload>
                              <el-button v-if="!isReadOnly" link type="danger" icon="Delete"
                                @click="houseForm.extend.propertyDeedUrl = ''">删除</el-button>
                            </template>
                            <template v-else>
                              <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file"
                                :data="{ category: 'DEED' }" :show-file-list="false"
                                :on-success="handleDeedUploadSuccess" :disabled="isReadOnly">
                                <el-button :disabled="isReadOnly" type="primary" plain icon="Plus" size="small"
                                  class="!rounded-lg">点击上传房本照片</el-button>
                              </el-upload>
                            </template>
                          </div>
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </div>

                  <!-- 3. 租房扩展 (Type 3) -->
                  <div v-else-if="houseForm.houseType === 3"
                    class="bg-blue-50/20 p-6 rounded-[32px] border border-blue-100/50">
                    <el-row :gutter="24">
                      <el-col :span="12">
                        <el-form-item label="月租金 (元/月)">
                          <el-input-number v-model="houseForm.extend.monthlyRent" class="!w-full"
                            controls-position="right" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="押金金额 (元)">
                          <el-input-number v-model="houseForm.extend.depositAmount" class="!w-full"
                            controls-position="right" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="12">
                        <el-form-item label="押金方式">
                          <el-input v-model="houseForm.extend.depositMethod" placeholder="如: 押一付三"
                            class="google-input" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="12">
                        <el-form-item label="可入住日期">
                          <el-date-picker v-model="houseForm.extend.checkInDate" type="date" value-format="YYYY-MM-DD"
                            placeholder="选择日期" class="!w-full google-input" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="8">
                        <el-form-item label="出租方式">
                          <el-radio-group v-model="houseForm.extend.rentType" class="google-radio-group-alt">
                            <el-radio-button :value="1">整租</el-radio-button>
                            <el-radio-button :value="2">合租</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="最短租期 (月)">
                          <el-input-number v-model="houseForm.extend.minLeasePeriod" :min="1" class="!w-full"
                            controls-position="right" />
                        </el-form-item>
                      </el-col>
                      <el-col :span="8">
                        <el-form-item label="支持短租">
                          <el-radio-group v-model="houseForm.extend.supportShortRent" class="google-radio-group-alt">
                            <el-radio-button :value="1">是</el-radio-button>
                            <el-radio-button :value="0">否</el-radio-button>
                          </el-radio-group>
                        </el-form-item>
                      </el-col>
                    </el-row>
                    <el-row :gutter="24" class="mt-4">
                      <el-col :span="24">
                        <el-form-item label="配套设施">
                          <el-input v-model="houseForm.extend.appliances" placeholder="如: 洗衣机, 冰箱, 宽带"
                            class="google-input" />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="详细描述" name="desc">
              <div class="mt-6">
                <el-form-item label="房源卖点描述" label-width="0">
                  <el-input v-model="houseForm.description" type="textarea" :rows="15" placeholder="请输入房源描述"
                    class="google-input !rounded-3xl" />
                </el-form-item>
              </div>
            </el-tab-pane>

            <el-tab-pane label="房源相册" name="photos">
              <div class="mt-6 px-4">
                <div class="flex items-center justify-between mb-6">
                  <div>
                    <div class="flex items-center gap-3 mb-1">
                      <div class="w-2 h-8 bg-amber-500 rounded-full"></div>
                      <span class="text-lg font-black text-gray-800">房源实拍图</span>
                    </div>
                    <p class="text-xs text-gray-400 font-medium">第一张图片将默认作为列表封面展示</p>
                  </div>
                  <el-tag v-if="!isReadOnly" type="warning" size="small" class="!rounded-lg">支持多图上传</el-tag>
                </div>

                <div class="photo-manage-grid">
                  <el-upload v-model:file-list="housePhotos" :action="uploadUrl" :headers="uploadHeaders" name="file"
                    :data="{ category: 'HOUSE' }" list-type="picture-card" :on-preview="handlePictureCardPreview"
                    :on-success="handleUploadSuccess" :on-error="handleUploadError" :disabled="isReadOnly"
                    class="house-album-uploader">
                    <div class="flex flex-col items-center gap-1">
                      <el-icon :size="24" class="text-amber-500">
                        <Plus />
                      </el-icon>
                      <span class="text-[10px] text-gray-400 font-bold uppercase">添加照片</span>
                    </div>
                  </el-upload>
                </div>

                <div class="mt-8 p-6 bg-blue-50/30 rounded-[32px] border border-blue-100/50">
                  <div class="flex items-start gap-4">
                    <el-icon class="text-blue-500 mt-1" :size="20">
                      <InfoFilled />
                    </el-icon>
                    <div>
                      <h4 class="text-sm font-bold text-blue-900 mb-1">照片上传规范</h4>
                      <p class="text-xs text-blue-600/70 leading-relaxed">
                        请上传清晰的室内实拍图，包括客厅、卧室、厨房及卫生间。建议尺寸为 4:3 或 16:9，单张大小不超过 10MB。严禁上传带有其他平台水印或虚假宣传的图片。
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane v-if="isReadOnly" label="交易动态" name="trade">
              <div v-loading="tradeLoading" class="mt-6 px-4 min-h-[400px]">
                <el-timeline v-if="tradeList.length > 0">
                  <el-timeline-item v-for="(item, index) in tradeList" :key="index" :type="item.type"
                    :timestamp="item.time">
                    <div class="bg-gray-50/80 p-4 rounded-2xl border border-gray-100">
                      <div class="flex items-center justify-between">
                        <span class="font-bold text-gray-900">{{ item.title }}</span>
                        <el-tag :type="item.tagType" size="small">{{ item.status }}</el-tag>
                      </div>
                      <p class="text-xs text-gray-600 mt-2">{{ item.content }}</p>
                      <p v-if="item.operator" class="text-[11px] text-gray-400 mt-1.5">操作人：{{ item.operator }}</p>
                    </div>
                  </el-timeline-item>
                </el-timeline>
                <el-empty v-else description="暂无历史交易动态记录" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-form>
      </div>

      <template #footer>
        <!-- 审核模式下的页脚 -->
        <div v-if="isReview"
          class="p-6 border-t bg-white absolute bottom-0 left-0 right-0 z-10 shadow-[0_-4px_20px_rgba(0,0,0,0.05)]">
          <div class="flex flex-col gap-4">
            <div class="flex items-center justify-between">
              <span class="text-sm font-bold text-gray-700">状态变更操作</span>
              <el-radio-group v-model="auditForm.auditStatus" class="google-radio-group-alt">
                <el-radio-button :value="1">审核通过 (在售)</el-radio-button>
                <el-radio-button :value="4">审核拒绝 (下架)</el-radio-button>
                <el-radio-button :value="0">待定 (待审核)</el-radio-button>
              </el-radio-group>
            </div>
            <el-input v-model="auditForm.reason" type="textarea" :rows="2" placeholder="请填写审核意见或变更原因..."
              class="google-input !rounded-2xl" />
            <div class="flex justify-end gap-3 mt-2">
              <el-button @click="drawerVisible = false" class="!rounded-full px-8">取消</el-button>
              <el-button v-hasPermi="['house:house:audit']" type="primary" @click="submitAudit"
                class="!bg-[#1a73e8] border-none !rounded-full px-12 font-bold shadow-lg shadow-blue-100">确认变更/审核</el-button>
            </div>
          </div>
        </div>
        <!-- 普通模式下的页脚 -->
        <div v-else
          class="flex items-center justify-end gap-3 px-8 py-6 border-t bg-white absolute bottom-0 left-0 right-0 z-10">
          <el-button @click="drawerVisible = false" class="!rounded-full px-8">{{ isReadOnly ? '关闭' : '取消'
            }}</el-button>
          <el-button v-if="!isReadOnly" v-hasPermi="['house:house:save']" type="primary" @click="handleSave"
            class="!bg-[#1a73e8] border-none !rounded-full px-12">保存房源</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 图片大图预览 -->
    <el-dialog v-model="previewVisible" class="preview-dialog" width="800px" append-to-body>
      <img :src="previewImageUrl" class="w-full rounded-2xl shadow-2xl" alt="房源照片预览" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import {
  Plus, List, Grid, LocationFilled,
  CaretTop, CaretBottom, Histogram, Location, Clock,
  MoreFilled, InfoFilled, WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listHouse, saveHouse, auditHouse, getHouse, exportHouse, getHouseStats, type House, type HouseStats } from '@/api/house/house'
import { listProject } from '@/api/house/project'
import { getSalesOptions } from '@/api/system/user'
import { listStatusLog } from '@/api/house/log'
import { listTransactions } from '@/api/house/trade'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const canAssignSales = computed(() => userStore.permissions.includes('house:house:assign'))
const currentUserId = computed(() => userStore.userInfo?.id)

const loading = ref(false)
const houseRef = ref<any>(null)
const houseList = ref<House[]>([])
const total = ref(0)
const viewMode = ref('list')

// --- 下拉选项数据 ---
const projects = ref<any[]>([])
const salesOptions = ref<any[]>([])

const stats = ref<{ title: string; value: string; trend: number; icon: string; iconColor: string }[]>([])
const statsLoading = ref(true)

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await getHouseStats()
    const d = res.data
    stats.value = [
      { title: '在线房源', value: d.onlineCount.toLocaleString(), trend: d.onlineTrend, icon: 'Histogram', iconColor: 'text-blue-500' },
      { title: '本月新增', value: d.monthlyNewCount.toLocaleString(), trend: d.monthlyNewTrend, icon: 'Plus', iconColor: 'text-green-500' },
      { title: '待审核', value: d.pendingReviewCount.toLocaleString(), trend: d.pendingReviewTrend, icon: 'WarningFilled', iconColor: 'text-orange-500' },
      { title: '成交均价', value: '¥' + Number(d.avgDealPrice).toFixed(1) + 'W', trend: d.avgDealPriceTrend, icon: 'Histogram', iconColor: 'text-purple-500' }
    ]
  } catch {
    stats.value = []
  } finally {
    statsLoading.value = false
  }
}

const houseTags = [
  '南北通透', '满五唯一', '带电梯', '近地铁', '精装修',
  '采光充足', '名校周边', '环境优美', '随时看房', '拎包入住'
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  houseNo: '',
  roomNo: '',
  houseType: null,
  status: null,
  city: ''
})

/** 查询房源列表 */
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await listHouse(queryParams)
    houseList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 重置查询 */
const resetQuery = () => {
  queryParams.houseNo = ''
  queryParams.roomNo = ''
  queryParams.houseType = null
  queryParams.status = null
  queryParams.city = ''
  handleQuery()
}

/** 获取项目列表 */
const getProjectList = async () => {
  const res = await listProject({ pageNum: 1, pageSize: 100 })
  projects.value = res.data.records.map((p: any) => ({ id: p.id, name: p.projectName }))
}

/** 获取销售列表 */
const getSalesList = async () => {
  const res = await getSalesOptions()
  salesOptions.value = (res.data as any[]).map((u: any) => ({ id: u.id, name: u.realName }))
}

const getHouseTypeText = (type: number) => {
  const map: any = { 1: '新房', 2: '二手房', 3: '租房' }
  return map[type] || '其他'
}

const getHouseTypeTag = (type: number) => {
  const map: any = { 1: 'danger', 2: 'success', 3: 'warning' }
  return map[type] || 'info'
}

const getStatusText = (status: number) => {
  const map: any = { 0: '待审核', 1: '在售', 2: '已预订', 3: '已成交', 4: '下架' }
  return map[status] || '未知'
}

const getStatusTag = (status: number) => {
  const map: any = { 0: 'info', 1: 'success', 2: 'warning', 3: 'primary', 4: 'danger' }
  return map[status] || 'info'
}

const getPriceUnitText = (unit: number) => {
  const map: any = { 1: '元/㎡', 2: '万元', 3: '元/月' }
  return map[unit] || ''
}

const getTagColorClass = (index: number) => {
  const colors = ['!bg-blue-50 !text-blue-600', '!bg-green-50 !text-green-600', '!bg-orange-50 !text-orange-600', '!bg-purple-50 !text-purple-600']
  return colors[index % colors.length]
}

// --- 抽屉逻辑 ---
const drawerVisible = ref(false)
const dialogTitle = ref('')
const activeTab = ref('basic')
const isReadOnly = ref(false)
const isReview = ref(false)
const tradeLoading = ref(false)
const tradeList = ref<any[]>([])

// --- 照片管理 ---
const housePhotos = ref<any[]>([])
const previewVisible = ref(false)
const previewImageUrl = ref('')

// --- 上传配置 ---
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/v1/common/upload'
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('resms_token')
}))

const handlePictureCardPreview = (file: any) => {
  previewImageUrl.value = file.url || ''
  previewVisible.value = true
}

const handleUploadSuccess = (response: any, file: any) => {
  if (response.code === 200) {
    file.url = response.data.url // 后端已返回完整可用 URL，无需前端拼接
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const handleDeedUploadSuccess = (res: any) => {
  if (res.code === 200) {
    houseForm.extend.propertyDeedUrl = res.data.url
    ElMessage.success('房本照片上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

const houseForm = reactive<any>({
  id: undefined,
  projectId: undefined,
  salesId: undefined,
  buildingNo: '',
  unitNo: '',
  roomNo: '',
  houseType: 1,
  layout: '',
  floor: undefined,
  totalFloor: undefined,
  orientation: '',
  decoration: '',
  area: undefined,
  tags: [],
  price: undefined,
  priceUnit: 1,
  coordinate: { lng: null, lat: null },
  extend: {},
  description: ''
})

const priceSuffix = computed(() => {
  const map: Record<number, string> = { 1: '元/㎡', 2: '万元', 3: '元/月' }
  return map[houseForm.priceUnit] || ''
})

/** 表单校验规则 */
const houseRules = {
  projectId: [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  roomNo: [{ required: true, message: '请输入房号', trigger: 'blur' }],
  houseType: [{ required: true, message: '请选择房源类型', trigger: 'change' }],
  layout: [{ required: true, message: '请输入户型规格，如: 3室2厅2卫', trigger: 'blur' }],
  floor: [{ required: true, type: 'number', message: '请输入所在楼层', trigger: 'change' }],
  totalFloor: [{ required: true, type: 'number', message: '请输入总楼层', trigger: 'change' }],
  area: [{ required: true, type: 'number', message: '请输入建筑面积', trigger: 'change' }],
  price: [{ required: true, type: 'number', message: '请输入价格', trigger: 'change' }],
  priceUnit: [{ required: true, message: '请选择价格单位', trigger: 'change' }]
}

const resetForm = () => {
  Object.assign(houseForm, {
    id: undefined,
    projectId: undefined,
    salesId: undefined,
    buildingNo: '',
    unitNo: '',
    roomNo: '',
    houseType: 1,
    layout: '',
    floor: undefined,
    totalFloor: undefined,
    orientation: '',
    decoration: '',
    area: undefined,
    tags: [],
    price: undefined,
    priceUnit: 1,
    coordinate: { lng: null, lat: null },
    extend: {},
    description: ''
  })
  housePhotos.value = []
}

const handleAdd = () => {
  dialogTitle.value = '新增房源'
  isReadOnly.value = false
  isReview.value = false
  resetForm()
  // 如果没有指派权限，默认选中自己
  if (!canAssignSales.value) {
    houseForm.salesId = currentUserId.value
  }
  drawerVisible.value = true
}

const handleUpdate = async (row: any) => {
  dialogTitle.value = '编辑房源'
  isReadOnly.value = false
  isReview.value = false
  const res = await getHouse(row.id)
  const data = res.data
  // 基础数据映射
  Object.assign(houseForm, data.house)
  // 显式映射展示价格字段（优先取 VO 顶层，兼容后端 HouseVO 重构）
  if (data.price !== undefined) houseForm.price = data.price
  if (data.priceUnit !== undefined) houseForm.priceUnit = data.priceUnit
  if (data.unitPrice !== undefined) houseForm.unitPrice = data.unitPrice

  if (data.house.houseType === 1) houseForm.extend = data.newHouseExtend || {}
  else if (data.house.houseType === 2) houseForm.extend = data.secondHouseExtend || {}
  else if (data.house.houseType === 3) houseForm.extend = data.rentHouseExtend || {}
  houseForm.coordinate = { lng: data.house.longitude, lat: data.house.latitude }
  // 处理图片回显：后端已返回完整 URL
  housePhotos.value = (data.images || []).map((img: any) => ({
    name: String(img.id),
    url: img.url || img.fileKey // 后端已返回完整可用 URL
  }))
  drawerVisible.value = true
}

const handleView = async (row: any) => {
  await handleUpdate(row)
  dialogTitle.value = '房源详情'
  isReadOnly.value = true
  isReview.value = false
}

const handleSave = async () => {
  // 1. 表单校验
  const formEl = houseRef.value
  if (!formEl) return
  const valid = await formEl.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请完善必填信息后再提交')
    return
  }

  loading.value = true
  try {
    const payload = JSON.parse(JSON.stringify(houseForm))

    // 2. 坐标字段展开：前端用 { lng, lat } 存储，后端接收 longitude/latitude
    payload.longitude = houseForm.coordinate?.lng ?? null
    payload.latitude = houseForm.coordinate?.lat ?? null
    delete payload.coordinate  // 不传原始对象，避免后端解析异常

    // 3. 构造图片列表 —— 后端已返回完整 URL，前端直接传递，由后端做路径清理
    payload.images = housePhotos.value.map((p, index) => ({
      url: p.response ? p.response.data.url : (p.url || ''),
      sortOrder: index + 1,
      imageType: index === 0 ? 1 : 2,
      isDefault: index === 0 ? 1 : null // 绝不传 0，避免唯一索引冲突
    }))

    // 调用后端接口保存
    await saveHouse(payload)
    ElMessage.success('保存成功')
    drawerVisible.value = false
    handleQuery()
    fetchStats()
  } finally {
    loading.value = false
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除房源 "${row.houseNo}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const payload = { id: row.id, isDeleted: 1 }
    await saveHouse(payload)
    ElMessage.success('删除成功')
    handleQuery()
    fetchStats()
  })
}

// --- 审核逻辑 ---
const auditForm = reactive({
  id: 0,
  auditStatus: 1,
  reason: ''
})

/** 审核/状态变更入口（复用 handleUpdate 加载详情，叠加审核模式） */
const handleStatusAudit = async (row: any) => {
  if (row.status === 2 || row.status === 3) {
    ElMessage.info('已预订或已成交房源不允许手动变更状态')
    return
  }
  loading.value = true
  try {
    await handleUpdate(row)
    isReadOnly.value = true
    isReview.value = true
    auditForm.id = row.id
    auditForm.auditStatus = row.status === 0 ? 1 : row.status
    auditForm.reason = ''
    dialogTitle.value = '房源详情及状态变更'
  } finally {
    loading.value = false
  }
}

const submitAudit = async () => {
  if (!auditForm.reason) {
    ElMessage.warning('请输入审核原因或备注')
    return
  }
  await auditHouse(auditForm)
  ElMessage.success('审核/状态变更完成')
  drawerVisible.value = false
  handleQuery()
  fetchStats()
}

const handleExport = () => {
  exportHouse(queryParams).then(res => {
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `房源列表_${new Date().getTime()}.xlsx`
    link.click()
  })
}

const handlePickMap = () => {
  houseForm.coordinate.lng = 113.264434
  houseForm.coordinate.lat = 23.129162
  ElMessage.success('坐标已拾取')
}

/** 房源状态映射 */
const houseStatusMap: Record<number, string> = {
  0: '待审核', 1: '在售', 2: '已预订', 3: '已成交', 4: '下架'
}
const getHouseStatusText = (s: number | null) => s != null ? (houseStatusMap[s] || '未知') : '无'

/** 交易状态映射 */
const txStatusMap: Record<number, string> = {
  0: '待付定金', 1: '已付定金', 2: '已付首付', 3: '已过户', 4: '已完成', 5: '已取消'
}
const getTxStatusText = (s: number) => txStatusMap[s] || '未知'

const loadTradeDynamics = async () => {
  const houseId = houseForm.id
  if (!houseId) return

  tradeLoading.value = true
  tradeList.value = []

  try {
    const [logSettled, txSettled] = await Promise.allSettled([
      listStatusLog({ houseId, pageNum: 1, pageSize: 200 }),
      listTransactions({ houseId, pageNum: 1, pageSize: 50 })
    ])

    const items: any[] = []

    // 1. 房源状态变更日志
    if (logSettled.status === 'fulfilled') {
      const logs = logSettled.value.data?.records || []
      for (const log of logs) {
        let tagType = 'info'
        let type = 'info'
        if (log.toStatus === 3) { tagType = 'success'; type = 'success' }
        else if (log.toStatus === 4) { tagType = 'danger'; type = 'danger' }

        items.push({
          title: '状态变更',
          content: `房源状态从[${getHouseStatusText(log.fromStatus)}]变更为[${getHouseStatusText(log.toStatus)}]${log.changeReason ? '。原因：' + log.changeReason : '。'}`,
          time: log.createTime,
          operator: log.operatorName,
          status: getHouseStatusText(log.toStatus),
          type,
          tagType
        })
      }
    }

    // 2. 交易记录
    if (txSettled.status === 'fulfilled') {
      const txs = txSettled.value.data?.records || []
      for (const tx of txs) {
        const customerName = tx.customer?.realName || '未知客户'
        const salesName = tx.sales?.realName || '未知'

        let type = 'primary'
        let tagType = 'primary'
        if (tx.status === 5) { type = 'danger'; tagType = 'danger' }
        else if (tx.status === 4) { type = 'success'; tagType = 'success' }

        items.push({
          title: '交易动态',
          content: `客户 ${customerName} 以 ¥${tx.dealPrice?.toLocaleString() ?? '?'} ${getTxTypeText(tx.paymentType)}，销售：${salesName}。交易编号：${tx.transactionNo}`,
          time: tx.createTime,
          operator: salesName,
          status: getTxStatusText(tx.status),
          type,
          tagType
        })
      }
    }

    // 按时间倒序排列
    items.sort((a: any, b: any) => b.time?.localeCompare(a.time || ''))

    tradeList.value = items
  } catch {
    tradeList.value = []
  } finally {
    tradeLoading.value = false
  }
}

const getTxTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '一次性付款', 2: '分期付款', 3: '按揭贷款', 4: '租房' }
  return map[type] || ''
}

watch(activeTab, (val) => {
  if (val === 'trade') loadTradeDynamics()
})

onMounted(() => {
  handleQuery()
  getProjectList()
  getSalesList()
  fetchStats()
})
</script>

<style scoped>
.house-management-container {
  height: calc(100vh - 84px);
  overflow-y: auto;
}

:deep(.google-input .el-input__wrapper) {
  border-radius: 12px;
}

:deep(.google-drawer .el-drawer__header) {
  padding: 24px 32px;
  border-bottom: 1px solid #f8fafc;
}

.google-radio-group-alt :deep(.el-radio-button__inner) {
  border-radius: 12px !important;
}

.house-form-drawer :deep(.el-form-item__label) {
  font-weight: 600;
}

.google-input-tags :deep(.el-select__wrapper) {
  border-radius: 16px;
  min-height: 52px;
  background-color: #f8fafc;
}
</style>
