<template>
  <div class="p-6 min-h-screen bg-gray-50/50">
    <!-- 头部统计卡片 (Material 3 风格) -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      <template v-if="statsLoading">
        <div v-for="i in 4" :key="i"
          class="bg-white p-6 rounded-[32px] border border-gray-100 shadow-sm animate-pulse">
          <div class="flex items-center justify-between mb-2">
            <div class="h-12 w-12 bg-gray-200 rounded-2xl"></div>
            <div class="h-3 w-16 bg-gray-200 rounded-full"></div>
          </div>
          <div class="flex items-baseline gap-2 mt-4">
            <div class="h-8 w-20 bg-gray-200 rounded-lg"></div>
            <div class="h-4 w-12 bg-gray-100 rounded-full"></div>
          </div>
        </div>
      </template>
      <div v-for="stat in projectStats" :key="stat.label"
        class="bg-white p-6 rounded-[32px] border border-gray-100 shadow-sm hover:shadow-md transition-all group">
        <div class="flex items-center justify-between mb-2">
          <div :class="`p-3 rounded-2xl ${stat.bgColor} ${stat.color} transition-transform group-hover:scale-110`">
            <el-icon :size="24">
              <component :is="stat.icon" />
            </el-icon>
          </div>
          <span class="text-xs font-bold text-gray-400 uppercase tracking-wider">{{ stat.label }}</span>
        </div>
        <div class="flex items-baseline gap-2">
          <span class="text-3xl font-black text-gray-800">{{ stat.value }}</span>
          <span v-if="stat.trend" class="text-xs font-bold" :class="stat.trend.startsWith('+') ? 'text-green-500' : 'text-red-500'">{{ stat.trend }}</span>
        </div>
      </div>
    </div>

    <!-- 主容器 -->
    <el-card class="!border-none !rounded-[40px] shadow-sm overflow-hidden">
      <!-- 搜索与操作栏 -->
      <div class="p-6 border-b border-gray-50">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div class="flex flex-wrap items-center gap-4">
            <div class="relative group">
              <el-input v-model="queryParams.projectName" placeholder="搜索楼盘、小区、开发商..." class="!w-72 google-input-search"
                clearable @keyup.enter="handleQuery">
                <template #prefix>
                  <el-icon class="text-gray-400 group-hover:text-blue-500 transition-colors">
                    <Search />
                  </el-icon>
                </template>
              </el-input>
            </div>

            <el-select v-model="queryParams.projectType" placeholder="项目类型" class="!w-32 google-input" clearable @change="handleQuery">
              <el-option label="新房楼盘" :value="1" />
              <el-option label="二手房小区" :value="2" />
            </el-select>

            <el-select v-model="queryParams.status" placeholder="销售状态" class="!w-32 google-input" clearable @change="handleQuery">
              <el-option label="在售" :value="1" />
              <el-option label="待售" :value="3" />
              <el-option label="售罄" :value="2" />
            </el-select>

            <div class="flex gap-2">
              <el-button type="primary" icon="Search"
                class="!rounded-2xl !px-6 !bg-blue-600 border-none shadow-sm shadow-blue-100"
                @click="handleQuery">查询</el-button>
              <el-button icon="Refresh" class="!rounded-2xl !px-6" @click="resetQuery">重置</el-button>
            </div>
          </div>

          <div class="flex items-center gap-3">
            <el-button v-hasPermi="['house:project:export']"
              class="!rounded-2xl !h-12 !px-6 !border-none !bg-gray-100 hover:!bg-gray-200 !text-gray-700 font-bold transition-all"
              @click="handleExport">
              <el-icon class="mr-2">
                <Download />
              </el-icon> 导出
            </el-button>
            <el-button v-hasPermi="['house:project:save']" type="primary"
              class="!rounded-2xl !h-12 !px-8 !border-none !bg-blue-600 hover:!bg-blue-700 shadow-lg shadow-blue-200 font-bold transition-all"
              @click="handleAdd">
              <el-icon class="mr-2">
                <Plus />
              </el-icon> 新增项目
            </el-button>
          </div>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="projectList" v-loading="loading" class="resms-table"
        :header-cell-style="{ background: '#fcfcfd', color: '#94a3b8', fontSize: '12px', fontWeight: '800', textTransform: 'uppercase', padding: '20px 0' }">
        <el-table-column label="项目信息" min-width="300">
          <template #default="{ row }">
            <div class="flex items-center gap-4 py-2">
              <el-image :src="row.coverUrl"
                class="w-20 h-20 rounded-2xl shadow-inner border border-gray-100" fit="cover">
                <template #error>
                  <div class="w-full h-full bg-gray-100 flex items-center justify-center">
                    <el-icon class="text-gray-300" :size="30">
                      <Picture />
                    </el-icon>
                  </div>
                </template>
              </el-image>
              <div class="flex flex-col gap-1">
                <span class="text-base font-black text-gray-800 hover:text-blue-600 cursor-pointer transition-colors">{{
                  row.projectName }}</span>
                <div class="flex items-center gap-2">
                  <span class="text-[10px] bg-gray-100 text-gray-500 px-2 py-0.5 rounded-md font-mono">{{ row.projectNo
                    }}</span>
                  <el-tag :type="row.projectType === 1 ? 'danger' : 'success'" size="small" effect="plain"
                    class="!rounded-lg !border-none !bg-opacity-10 !font-bold">
                    {{ row.projectType === 1 ? '新房楼盘' : '二手房小区' }}
                  </el-tag>
                </div>
                <div class="flex items-center text-xs text-gray-400">
                  <el-icon class="mr-1">
                    <Location />
                  </el-icon>
                  {{ row.city }} · {{ row.district }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="详细地址" min-width="200" prop="address">
          <template #default="{ row }">
            <span class="text-sm text-gray-600 font-medium line-clamp-2">{{ row.address }}</span>
          </template>
        </el-table-column>

        <el-table-column label="开发商/物业" min-width="180">
          <template #default="{ row }">
            <div class="flex flex-col gap-1">
              <span class="text-sm font-bold text-gray-700">{{ row.developer }}</span>
              <span class="text-xs text-gray-400">{{ row.propertyCompany }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="核心指标" min-width="220">
          <template #default="{ row }">
            <div class="grid grid-cols-2 gap-y-2 gap-x-4">
              <div class="flex flex-col">
                <span class="text-[10px] text-gray-400 font-bold uppercase">容积率</span>
                <span class="text-xs font-black text-gray-700">{{ row.plotRatio }}</span>
              </div>
              <div class="flex flex-col">
                <span class="text-[10px] text-gray-400 font-bold uppercase">绿化率</span>
                <span class="text-xs font-black text-green-600">{{ row.greeningRate }}%</span>
              </div>
              <div class="flex flex-col">
                <span class="text-[10px] text-gray-400 font-bold uppercase">总户数</span>
                <span class="text-xs font-black text-gray-700">{{ row.totalHouseholds }}户</span>
              </div>
              <div class="flex flex-col">
                <span class="text-[10px] text-gray-400 font-bold uppercase">佣金比例</span>
                <span class="text-xs font-black text-orange-600">{{ row.commissionRate }}%</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="销售状态" width="120" align="center">
          <template #default="{ row }">
            <div
              :class="`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full font-bold text-xs ${getStatusClass(row.status)}`">
              <span class="w-1.5 h-1.5 rounded-full bg-current"></span>
              {{ getStatusLabel(row.status) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="right" fixed="right">
          <template #default="{ row }">
            <div class="flex gap-1 justify-center">
              <el-tooltip v-if="checkPermi(['house:project:query'])" content="查看详情" placement="top">
                <el-button circle class="!border-none hover:!bg-blue-50 hover:!text-blue-600" @click="handleView(row)">
                  <el-icon :size="18">
                    <View />
                  </el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip v-if="checkPermi(['house:project:save'])" content="编辑项目" placement="top">
                <el-button circle class="!border-none hover:!bg-amber-50 hover:!text-amber-600"
                  @click="handleEdit(row)">
                  <el-icon :size="18">
                    <EditPen />
                  </el-icon>
                </el-button>
              </el-tooltip>
              <el-dropdown v-if="checkPermi(['house:project:log']) || checkPermi(['house:project:delete'])" trigger="click">
                <el-button circle class="!border-none hover:!bg-gray-100">
                  <el-icon :size="18">
                    <MoreFilled />
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu class="!rounded-2xl !p-2 !border-none shadow-xl">

                    <el-dropdown-item v-if="checkPermi(['house:project:log'])" class="!rounded-xl !py-2" @click="handleViewLog(row)"><el-icon>
                        <Tickets />
                      </el-icon>变动日志</el-dropdown-item>
                    <el-dropdown-item v-if="checkPermi(['house:project:delete'])" divided
                      class="!rounded-xl !py-2 !text-red-500" @click="handleDelete(row)"><el-icon>
                        <Delete />
                      </el-icon>删除项目</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 (修复) -->
      <div class="p-8 flex justify-between items-center bg-gray-50/30">
        <span class="text-xs font-bold text-gray-400">显示数据，共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          layout="total, sizes, prev, pager, next" :total="total" class="resms-pagination" @size-change="handleQuery"
          @current-change="handleQuery" />
      </div>
    </el-card>

    <!-- 详情查看抽屉 (Premium Design) -->
    <el-drawer v-model="detailVisible" size="560px" class="project-detail-drawer" :with-header="false">
      <div v-if="detailData" class="h-full flex flex-col bg-slate-50/50">
        <!-- 详情页头部 -->
        <div class="relative h-72 w-full overflow-hidden shrink-0">
          <el-image :src="detailData.coverUrl" class="w-full h-full object-cover">
            <template #error>
              <div class="w-full h-full bg-slate-200 flex items-center justify-center">
                <el-icon :size="48" class="text-slate-400">
                  <Picture />
                </el-icon>
              </div>
            </template>
          </el-image>
          <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent"></div>
          <el-button circle
            class="absolute top-6 right-6 !bg-white/20 !border-none !text-white hover:!bg-white/40 !backdrop-blur-md"
            @click="detailVisible = false">
            <el-icon :size="20">
              <Close />
            </el-icon>
          </el-button>

          <div class="absolute bottom-8 left-8 right-8">
            <div class="flex items-center gap-3 mb-2">
              <span
                :class="`px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-widest shadow-sm ${getStatusClass(detailData.status)}`">
                {{ getStatusLabel(detailData.status) }}
              </span>
              <span class="text-white/60 text-xs font-bold font-mono tracking-tighter">{{ detailData.projectNo }}</span>
            </div>
            <h1 class="text-3xl font-black text-white tracking-tight">{{ detailData.projectName }}</h1>
          </div>
        </div>

        <!-- 详情内容区 -->
        <div class="flex-1 overflow-y-auto p-8 -mt-6 relative z-10">
          <div class="bg-white rounded-[40px] shadow-xl shadow-slate-200/50 p-8 space-y-10 border border-slate-100">
            <!-- 核心数据网格 -->
            <div class="grid grid-cols-2 gap-8">
              <div class="flex flex-col gap-1">
                <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">项目类型</span>
                <div class="bg-slate-50 p-4 rounded-3xl flex items-center gap-3">
                  <div class="w-10 h-10 bg-blue-100 rounded-2xl flex items-center justify-center text-blue-600">
                    <el-icon :size="20">
                      <OfficeBuilding />
                    </el-icon>
                  </div>
                  <span class="font-bold text-slate-700">{{ detailData.projectType === 1 ? '住宅' : '商业' }}</span>
                </div>
              </div>
              <div class="flex flex-col gap-1">
                <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">规划户数</span>
                <div class="bg-slate-50 p-4 rounded-3xl flex items-center gap-3">
                  <div class="w-10 h-10 bg-emerald-100 rounded-2xl flex items-center justify-center text-emerald-600">
                    <el-icon :size="20">
                      <UserFilled />
                    </el-icon>
                  </div>
                  <span class="font-bold text-slate-700 font-mono text-lg">{{ detailData.totalHouseholds }} <span
                      class="text-xs text-slate-400">户</span></span>
                </div>
              </div>
            </div>

            <!-- 参数指标 -->
            <div>
              <h3 class="text-xs font-black text-slate-800 uppercase tracking-[2px] mb-6 flex items-center gap-2">
                <div class="w-1.5 h-4 bg-blue-600 rounded-full"></div>
                核心参数指标
              </h3>
              <div class="bg-slate-50/50 rounded-[32px] p-6 border border-slate-100 grid grid-cols-3 gap-6">
                <div class="flex flex-col items-center gap-2 text-center">
                  <span class="text-[9px] font-bold text-slate-400 uppercase">容积率</span>
                  <span class="text-lg font-black text-slate-800 font-mono">{{ detailData.plotRatio }}</span>
                </div>
                <div class="flex flex-col items-center gap-2 text-center border-x border-slate-100">
                  <span class="text-[9px] font-bold text-slate-400 uppercase">绿化率</span>
                  <span class="text-lg font-black text-slate-800 font-mono">{{ detailData.greeningRate }}%</span>
                </div>
                <div class="flex flex-col items-center gap-2 text-center">
                  <span class="text-[9px] font-bold text-slate-400 uppercase">物业费</span>
                  <span class="text-sm font-black text-slate-800">{{ detailData.propertyFee }} <span
                      class="text-[10px] text-slate-400">元/㎡/月</span></span>
                </div>
              </div>
            </div>

            <!-- 企业信息 -->
            <div class="space-y-4">
              <div
                class="flex items-center justify-between p-5 rounded-3xl border border-slate-100 hover:bg-slate-50/50 transition-colors">
                <div class="flex items-center gap-4">
                  <div class="w-12 h-12 bg-amber-50 rounded-2xl flex items-center justify-center text-amber-600">
                    <el-icon :size="22">
                      <Suitcase />
                    </el-icon>
                  </div>
                  <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">开发商</p>
                    <p class="font-bold text-slate-800">{{ detailData.developer }}</p>
                  </div>
                </div>
              </div>
              <div
                class="flex items-center justify-between p-5 rounded-3xl border border-slate-100 hover:bg-slate-50/50 transition-colors">
                <div class="flex items-center gap-4">
                  <div class="w-12 h-12 bg-indigo-50 rounded-2xl flex items-center justify-center text-indigo-600">
                    <el-icon :size="22">
                      <Checked />
                    </el-icon>
                  </div>
                  <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">物业公司</p>
                    <p class="font-bold text-slate-800">{{ detailData.propertyCompany }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 地理位置预览 -->
            <div class="space-y-4">
              <h3 class="text-xs font-black text-slate-800 uppercase tracking-[2px] flex items-center gap-2">
                <div class="w-1.5 h-4 bg-emerald-500 rounded-full"></div>
                地理位置
              </h3>
              <div class="bg-slate-100 rounded-[32px] h-48 overflow-hidden relative group">
                <div v-if="detailData.longitude" class="w-full h-full flex flex-col items-center justify-center gap-3">
                  <el-icon :size="32" class="text-emerald-500">
                    <LocationFilled />
                  </el-icon>
                  <div class="text-center px-6">
                    <p class="text-sm font-bold text-slate-700 mb-1">{{ detailData.address }}</p>
                    <p class="text-[10px] text-slate-400 font-mono tracking-wider">{{ detailData.province }} {{
                      detailData.city }} {{ detailData.district }}</p>
                  </div>
                </div>
                <div v-else class="w-full h-full flex items-center justify-center text-slate-400 text-xs italic">
                  坐标位置未录入
                </div>
              </div>
            </div>

            <!-- 分销佣金 -->
            <div class="bg-rose-50/50 rounded-[32px] p-8 border border-rose-100/50 flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div
                  class="w-12 h-12 bg-rose-500 rounded-2xl flex items-center justify-center text-white shadow-lg shadow-rose-100">
                  <el-icon :size="24">
                    <GoldMedal />
                  </el-icon>
                </div>
                <div>
                  <p class="text-[10px] font-black text-rose-400 uppercase tracking-widest">分销佣金比例</p>
                  <p class="text-2xl font-black text-rose-600 font-mono">{{ detailData.commissionRate }}%</p>
                </div>
              </div>
              <el-button type="danger" round
                class="!bg-rose-500 !border-none !h-12 !px-8 font-bold shadow-lg shadow-rose-100">
                申请分销
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 项目录入/编辑抽屉 (Material 3) -->
    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="640px" class="google-drawer" :with-header="false">
      <div class="h-full flex flex-col">
        <!-- 头部 -->
        <div class="px-8 py-8 flex items-center justify-between border-b border-gray-50 bg-white sticky top-0 z-10">
          <div class="flex items-center gap-4">
            <div
              class="w-14 h-14 bg-blue-600 rounded-[20px] flex items-center justify-center text-white shadow-xl shadow-blue-100">
              <el-icon :size="28">
                <OfficeBuilding />
              </el-icon>
            </div>
            <div>
              <h3 class="text-2xl font-black text-gray-800 tracking-tight">{{ drawerTitle }}</h3>
              <p class="text-xs text-gray-400 font-bold uppercase tracking-widest mt-0.5">Project Information Management
              </p>
            </div>
          </div>
          <el-button circle @click="drawerVisible = false"
            class="!border-none !bg-gray-50 hover:!bg-gray-100 !w-12 !h-12">
            <el-icon :size="24">
              <Close />
            </el-icon>
          </el-button>
        </div>

        <!-- 内容 -->
        <div class="flex-1 overflow-y-auto p-8 bg-gray-50/20">
          <el-form :model="projectForm" label-position="top" class="google-form-compact">
            <!-- 封面图上传 (新增) -->
            <div
              class="bg-white p-8 rounded-[40px] border border-gray-100 shadow-sm mb-8 relative overflow-hidden group">
              <div class="flex items-center gap-3 mb-8">
                <div class="w-2 h-8 bg-amber-500 rounded-full"></div>
                <span class="text-lg font-black text-gray-800">项目画册与封面</span>
              </div>
              <div class="flex flex-wrap gap-4">
                <div v-if="projectForm.coverUrl"
                  class="relative w-40 h-40 rounded-[24px] overflow-hidden group-hover:shadow-lg transition-all border-2 border-amber-100">
                  <el-image v-if="projectForm.coverUrl" :src="projectForm.coverUrl"
                    class="w-full h-full object-cover rounded-2xl shadow-inner" />
                  <div
                    class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                    <el-button circle size="small" icon="Delete" class="!bg-white/20 !text-white !border-none"
                      @click="projectForm.coverUrl = ''" />
                  </div>
                </div>
                <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file" :data="{ category: 'PROJECT' }"
                  :show-file-list="false" :on-success="handleUploadSuccess" :on-error="handleUploadError"
                  class="project-uploader-card">
                  <div class="flex flex-col items-center gap-1">
                    <el-icon :size="24" class="text-amber-500">
                      <Plus />
                    </el-icon>
                    <span class="text-[10px] text-gray-400 font-bold uppercase">上传图片</span>
                  </div>
                </el-upload>
              </div>
              <p class="text-[10px] text-gray-400 mt-4 px-2 italic font-medium">推荐比例 4:3，支持 JPG/PNG/WebP 格式，单个图片不超过 5MB。
              </p>
            </div>

            <!-- 基础信息区 -->
            <div class="bg-white p-8 rounded-[40px] border border-gray-100 shadow-sm mb-8">
              <div class="flex items-center gap-3 mb-8">
                <div class="w-2 h-8 bg-blue-600 rounded-full"></div>
                <span class="text-lg font-black text-gray-800">核心属性</span>
              </div>

              <el-form-item label="项目名称">
                <el-input v-model="projectForm.projectName" placeholder="请输入楼盘或小区全称" class="google-input-elevated" />
              </el-form-item>

              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="项目编号">
                    <el-input v-model="projectForm.projectNo" readonly class="google-input-elevated" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="销售状态">
                    <el-select v-model="projectForm.status" class="w-full google-input-elevated">
                      <el-option label="在售" :value="1" />
                      <el-option label="售罄" :value="2" />
                      <el-option label="待售" :value="3" />
                      <el-option label="下架" :value="4" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="项目类型">
                    <el-radio-group v-model="projectForm.projectType" class="google-radio-group-modern">
                      <el-radio-button :value="1">新房楼盘</el-radio-button>
                      <el-radio-button :value="2">二手房小区</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="佣金比例 (%)">
                    <el-input-number v-model="projectForm.commissionRate" :min="0" :max="100" class="!w-full"
                      controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="项目标签">
                <el-select v-model="projectForm.tags" multiple filterable allow-create default-first-option
                  placeholder="选择或输入楼盘特色标签" class="w-full google-input-elevated-tags">
                  <el-option v-for="item in projectTags" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </div>

            <!-- 开发与规划 -->
            <div class="bg-white p-8 rounded-[40px] border border-gray-100 shadow-sm mb-8">
              <div class="flex items-center gap-3 mb-8">
                <div class="w-2 h-8 bg-emerald-500 rounded-full"></div>
                <span class="text-lg font-black text-gray-800">开发与规划</span>
              </div>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="开发商">
                    <el-input v-model="projectForm.developer" placeholder="企业全称" class="google-input-elevated" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="物业公司">
                    <el-input v-model="projectForm.propertyCompany" placeholder="物业服务商" class="google-input-elevated" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="总户数 (户)">
                    <el-input-number v-model="projectForm.totalHouseholds" :min="0" class="!w-full"
                      controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="物业费 (元/㎡/月)">
                    <el-input-number v-model="projectForm.propertyFee" :min="0" :precision="2" class="!w-full"
                      controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="24">
                <el-col :span="12">
                  <el-form-item label="容积率">
                    <el-input-number v-model="projectForm.plotRatio" :min="0" :precision="2" class="!w-full"
                      controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="绿化率 (%)">
                    <el-input-number v-model="projectForm.greeningRate" :min="0" :max="100" class="!w-full"
                      controls-position="right" />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>

            <!-- 地理定位 -->
            <div class="bg-white p-8 rounded-[40px] border border-gray-100 shadow-sm">
              <div class="flex items-center gap-3 mb-8">
                <div class="w-2 h-8 bg-rose-500 rounded-full"></div>
                <span class="text-lg font-black text-gray-800">地理定位</span>
              </div>
              <el-row :gutter="24">
                <el-col :span="24">
                  <el-form-item label="所在省市区">
                    <el-cascader v-model="regionValues" :options="regionOptions"
                      :props="{ label: 'label', value: 'value', children: 'children' }" placeholder="请选择省 / 市 / 区"
                      class="w-full google-input-elevated" filterable clearable @change="handleRegionChange" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="详细地址">
                <el-input v-model="projectForm.address" type="textarea" :rows="3" placeholder="请输入楼盘具体地理位置描述"
                  class="google-input-elevated !rounded-[24px]" />
              </el-form-item>

              <!-- 交互式地图拾取 (融合) -->
              <div
                class="mt-6 bg-rose-50/30 p-5 rounded-[24px] border-2 border-dashed border-rose-100 group cursor-pointer hover:bg-rose-50/50 transition-all"
                @click="handlePickMap">
                <div class="flex items-center justify-between mb-4">
                  <span class="text-sm font-black text-rose-600 flex items-center">
                    <el-icon class="mr-2" :size="18">
                      <Coordinate />
                    </el-icon> 地理坐标拾取
                  </span>
                  <el-tag v-if="projectForm.coordinate.lng" size="small"
                    class="!bg-rose-500 !text-white !border-none !rounded-lg font-bold">已定位</el-tag>
                  <el-tag v-else size="small"
                    class="!bg-gray-200 !text-gray-500 !border-none !rounded-lg font-bold">未采集</el-tag>
                </div>
                <div class="h-32 bg-white/60 rounded-[20px] flex items-center justify-center relative overflow-hidden">
                  <div v-if="projectForm.coordinate.lng"
                    class="text-center z-10 animate-in fade-in zoom-in duration-300">
                    <div class="text-rose-600 font-black text-lg leading-tight">坐标已就绪</div>
                    <div class="text-[10px] text-gray-400 font-mono mt-1">LNG: {{ projectForm.coordinate.lng }} / LAT:
                      {{
                        projectForm.coordinate.lat }}</div>
                  </div>
                  <div v-else class="text-gray-300 text-xs font-bold uppercase tracking-widest">点击开启地图交互拾取</div>
                  <el-icon class="absolute -right-4 -bottom-4 text-rose-100 opacity-20" :size="100">
                    <Location />
                  </el-icon>
                </div>
              </div>
            </div>
          </el-form>
        </div>

        <!-- 底部 -->
        <div class="p-8 border-t border-gray-50 bg-white flex items-center justify-end gap-4 sticky bottom-0 z-10">
          <el-button @click="drawerVisible = false"
            class="!rounded-2xl !h-14 !px-10 !border-none !bg-gray-50 hover:!bg-gray-100 font-bold text-gray-400 transition-all">放弃修改</el-button>
          <el-button v-hasPermi="['house:project:save']" type="primary" :loading="loading" @click="handleSave"
            class="!rounded-2xl !h-14 !px-14 !border-none !bg-blue-600 hover:!bg-blue-700 shadow-xl shadow-blue-100 font-bold transition-all">保存项目数据</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 变动日志抽屉 (Timeline Style) -->
    <el-drawer v-model="logDrawerVisible" size="560px" class="log-drawer" :with-header="false">
      <div class="h-full flex flex-col bg-slate-50/50">
        <!-- 头部 -->
        <div class="px-8 py-7 flex items-center justify-between border-b border-gray-50 bg-white shrink-0">
          <div class="flex items-center gap-4">
            <div
              class="w-12 h-12 bg-indigo-600 rounded-[18px] flex items-center justify-center text-white shadow-lg shadow-indigo-100">
              <el-icon :size="24">
                <Tickets />
              </el-icon>
            </div>
            <div>
              <h3 class="text-xl font-black text-gray-800 tracking-tight">变动日志</h3>
              <p class="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-0.5">{{ logProjectName }}</p>
            </div>
          </div>
          <el-button circle @click="logDrawerVisible = false"
            class="!border-none !bg-gray-50 hover:!bg-gray-100 !w-10 !h-10">
            <el-icon :size="20">
              <Close />
            </el-icon>
          </el-button>
        </div>

        <!-- 时间轴内容 -->
        <div class="flex-1 overflow-y-auto p-8" v-loading="logLoading">
          <template v-if="logList.length > 0">
            <div class="relative">
              <!-- 时间轴竖线 -->
              <div class="absolute left-[19px] top-3 bottom-3 w-0.5 bg-indigo-100/80"></div>
              <div v-for="item in logList" :key="item.id" class="relative pb-8 pl-14 last:pb-0">
                <!-- 时间轴圆点 -->
                <div class="absolute left-[13px] top-[5px] w-3.5 h-3.5 rounded-full border-[3px] border-indigo-300 bg-white">
                </div>
                <!-- 事件卡片 -->
                <div class="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm hover:shadow-md transition-all">
                  <div class="flex items-center justify-between mb-3">
                    <div class="flex items-center gap-2.5">
                      <el-avatar :size="26" class="!bg-indigo-50 !text-indigo-600 !font-black text-[11px]">
                        {{ item.operatorName?.substring(0, 1) || '?' }}
                      </el-avatar>
                      <span class="text-sm font-black text-gray-700">{{ item.operatorName }}</span>
                    </div>
                    <span class="text-[10px] text-gray-400 font-bold font-mono tracking-tight">{{ item.createTime }}</span>
                  </div>
                  <div class="bg-gray-50 rounded-xl p-4 border border-gray-50">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="text-[11px] text-gray-500 font-bold">修改了</span>
                      <span
                        class="text-[11px] font-black text-indigo-600 bg-indigo-50/80 px-2.5 py-0.5 rounded-lg">{{
                        item.fieldLabel }}</span>
                    </div>
                    <div class="flex items-center gap-2.5 text-sm">
                      <span
                        class="text-gray-400 line-through font-medium bg-white px-2.5 py-1 rounded-lg border border-gray-100">{{ item.oldValue || '(空)' }}</span>
                      <el-icon class="text-gray-300" :size="14">
                        <Right />
                      </el-icon>
                      <span class="text-gray-800 font-black bg-indigo-50/50 px-2.5 py-1 rounded-lg">{{ item.newValue }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- 分页 -->
            <div class="mt-6 flex justify-center" v-if="logTotal > logQueryParams.pageSize">
              <el-pagination v-model:current-page="logQueryParams.pageNum" v-model:page-size="logQueryParams.pageSize"
                layout="prev, pager, next" :total="logTotal" class="log-timeline-pagination"
                @current-change="fetchProjectLog" small />
            </div>
          </template>
          <el-empty v-else description="暂无变更记录" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import {
  Search, Plus, Download, View, EditPen, Delete, Picture,
  Location, MoreFilled, OfficeBuilding,
  Coordinate, Close, Tickets, Refresh, Right,
  House, Timer, PieChart, Checked
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProject, saveProject, delProject, getProject, getProjectStats, exportProject, type Project, type ProjectStats } from '@/api/house/project'
import { listProjectLog, type ProjectChangeLog } from '@/api/house/log'
import { checkPermi } from '@/utils/permission'
import regionData from '@/assets/group.json'

const regionOptions = regionData.data
const regionValues = ref<string[]>([])

const loading = ref(false)
const projectList = ref<Project[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailData = ref<Project | null>(null)

// 变动日志抽屉状态
const logDrawerVisible = ref(false)
const logList = ref<ProjectChangeLog[]>([])
const logTotal = ref(0)
const logLoading = ref(false)
const logProjectName = ref('')
const logQueryParams = reactive({
  projectId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const handleViewLog = async (row: Project) => {
  logProjectName.value = row.projectName || ''
  logQueryParams.projectId = row.id
  logQueryParams.pageNum = 1
  logDrawerVisible.value = true
  await fetchProjectLog()
}

const fetchProjectLog = async () => {
  if (!logQueryParams.projectId) return
  logLoading.value = true
  try {
    const res = await listProjectLog(logQueryParams)
    logList.value = res.data.records
    logTotal.value = res.data.total
  } finally {
    logLoading.value = false
  }
}

const projectStats = ref<{ label: string; value: string; icon: string; color: string; bgColor: string; trend: string }[]>([])
const statsLoading = ref(true)

const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await getProjectStats()
    const d = res.data
    const fmt = (v: number) => (v >= 0 ? '+' : '') + v.toFixed(1) + '%'
    projectStats.value = [
      { label: '项目总数', value: String(d.totalCount), icon: 'OfficeBuilding', color: 'text-blue-600', bgColor: 'bg-blue-50', trend: fmt(d.totalTrend) },
      { label: '在售楼盘', value: String(d.onSaleCount), icon: 'House', color: 'text-rose-600', bgColor: 'bg-rose-50', trend: fmt(d.onSaleTrend) },
      { label: '待开项目', value: String(d.pendingCount), icon: 'Timer', color: 'text-amber-600', bgColor: 'bg-amber-50', trend: fmt(d.pendingTrend) },
      { label: '平均佣金', value: d.avgCommissionRate + '%', icon: 'PieChart', color: 'text-emerald-600', bgColor: 'bg-emerald-50', trend: fmt(d.avgCommissionTrend) }
    ]
  } catch {
    projectStats.value = []
  } finally {
    statsLoading.value = false
  }
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  projectName: '',
  projectType: null,
  status: null
})

/** 查询项目列表 */
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await listProject(queryParams)
    projectList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/** 查询项目详情 */
const handleView = async (row: Project) => {
  if (!row.id) return
  loading.value = true
  try {
    const res = await getProject(row.id)
    detailData.value = res.data
    detailVisible.value = true
  } finally {
    loading.value = false
  }
}

/** 重置查询 */
const resetQuery = () => {
  queryParams.projectName = ''
  queryParams.projectType = null
  queryParams.status = null
  handleQuery()
}

const getStatusLabel = (status: number) => {
  const map: any = { 1: '在售', 2: '售罄', 3: '待售', 4: '下架' }
  return map[status] || '未知'
}

const getStatusClass = (status: number) => {
  const map: any = {
    1: 'bg-rose-50 text-rose-600',
    2: 'bg-gray-100 text-gray-500',
    3: 'bg-amber-50 text-amber-600',
    4: 'bg-red-50 text-red-600'
  }
  return map[status] || 'bg-gray-50 text-gray-400'
}

// 常用项目标签
const projectTags = [
  '地铁沿线', '学区房', '现房销售', '园林景观', '品牌开发商',
  '低密度', '闹中取静', '南北对流', '自带商业', '交通便利'
]

// --- 业务逻辑控制 ---
const drawerVisible = ref(false)
const drawerTitle = ref('')

// --- 上传配置 ---
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/v1/common/upload'
const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('resms_token')
}))

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    projectForm.coverUrl = response.data.url
    ElMessage.success('封面图上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('上传接口异常')
}

// 表单对象
const projectForm = reactive<any>({
  id: undefined,
  projectNo: '',
  projectName: '',
  projectType: 1,
  developer: '',
  propertyCompany: '',
  province: '广东省',
  city: '广州市',
  district: '',
  address: '',
  totalHouseholds: undefined,
  propertyFee: undefined,
  plotRatio: undefined,
  greeningRate: undefined,
  commissionRate: 0,
  status: 1,
  tags: [],
  coverUrl: '',
  coordinate: { lng: null, lat: null }
})

const handleAdd = () => {
  drawerTitle.value = '录入新项目'
  Object.assign(projectForm, {
    id: undefined,
    projectNo: '自动生成',
    projectName: '',
    projectType: 1,
    developer: '',
    propertyCompany: '',
    province: '广东省',
    city: '广州市',
    district: '',
    address: '',
    totalHouseholds: undefined,
    propertyFee: undefined,
    plotRatio: undefined,
    greeningRate: undefined,
    commissionRate: 0,
    status: 1,
    tags: [],
    coverUrl: '',
    coordinate: { lng: null, lat: null }
  })
  regionValues.value = []
  drawerVisible.value = true
}

const handleRegionChange = (values: any) => {
  if (values && values.length === 3) {
    projectForm.province = values[0]
    projectForm.city = values[1]
    projectForm.district = values[2]
  } else {
    projectForm.province = ''
    projectForm.city = ''
    projectForm.district = ''
  }
}

const handleEdit = (row: any) => {
  drawerTitle.value = '编辑项目信息'
  Object.assign(projectForm, JSON.parse(JSON.stringify(row)))
  // 映射坐标
  projectForm.coordinate = { lng: row.longitude, lat: row.latitude }
  // 映射地区回显
  if (row.province && row.city && row.district) {
    regionValues.value = [row.province, row.city, row.district]
  } else {
    regionValues.value = []
  }
  drawerVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除项目 "${row.projectName}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await delProject(row.id)
      ElMessage.success('删除成功')
      handleQuery()
      fetchStats()
    } catch (e: any) {
      // 后端已有校验提示，无需额外处理
    }
  }).catch(() => {})
}

const handleExport = () => {
  exportProject(queryParams).then(res => {
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = `项目列表_${new Date().getTime()}.xlsx`
    link.click()
  })
}

const handleSave = async () => {
  loading.value = true
  try {
    // 映射坐标回 DTO 格式 (这里我们在 API 层处理或直接在 payload 处理)
    const payload = { ...projectForm }
    payload.longitude = projectForm.coordinate.lng
    payload.latitude = projectForm.coordinate.lat
    delete payload.coordinate

    await saveProject(payload)
    ElMessage.success(drawerTitle.value + '成功')
    drawerVisible.value = false
    handleQuery()
    fetchStats()
  } finally {
    loading.value = false
  }
}

const handlePickMap = () => {
  // 模拟地图拾取
  projectForm.coordinate.lng = 113.264434
  projectForm.coordinate.lat = 23.129162
  ElMessage({
    message: '地理坐标拾取成功',
    type: 'success',
    duration: 2000,
    offset: 80
  })
}

onMounted(() => {
  handleQuery()
  fetchStats()
})
</script>

<style scoped>
.google-input-search :deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: #f1f3f4;
  box-shadow: none !important;
  padding: 0 15px;
  height: 48px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.google-input-search :deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
}

.google-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  background-color: #fff;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  height: 48px;
}

.resms-table :deep(.el-table__row) {
  transition: all 0.3s;
}

.resms-table :deep(.el-table__row:hover) {
  background-color: #f8fafc !important;
  transform: scale(1.002);
}

.resms-table :deep(.el-table__cell) {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}

.resms-pagination :deep(.el-pager li) {
  border-radius: 12px;
  margin: 0 4px;
  font-weight: 800;
  color: #64748b;
}

.resms-pagination :deep(.el-pager li.is-active) {
  background-color: #1a73e8;
  color: #fff;
}

/* Google Elevated Form Styles */
.google-input-elevated :deep(.el-input__wrapper) {
  border-radius: 20px;
  background-color: #f8fafc;
  box-shadow: none !important;
  border: 2px solid transparent;
  height: 56px;
  padding: 0 20px;
  transition: all 0.3s;
}

.google-input-elevated :deep(.el-input__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
}

.google-radio-group-modern :deep(.el-radio-button__inner) {
  border-radius: 16px !important;
  border: none !important;
  margin-right: 8px;
  background-color: #f1f5f9;
  color: #64748b;
  font-weight: 800;
  padding: 12px 24px;
  transition: all 0.3s;
}

.google-radio-group-modern :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #1a73e8;
  color: #fff;
  box-shadow: 0 8px 16px -4px rgba(26, 115, 232, 0.3);
}

.google-drawer :deep(.el-drawer__body) {
  padding: 0;
}

/* 项目标签选择器美化 */
.google-input-elevated-tags :deep(.el-select__wrapper) {
  border-radius: 20px;
  background-color: #f8fafc;
  box-shadow: none !important;
  border: 2px solid transparent;
  min-height: 56px;
  padding: 4px 16px;
  transition: all 0.3s;
}

.google-input-elevated-tags :deep(.el-select__wrapper.is-focus) {
  background-color: #fff;
  border-color: #1a73e8;
}

.google-input-elevated-tags .el-tag {
  border-radius: 10px;
  background-color: #eff6ff;
  color: #1d4ed8;
  font-weight: 800;
  border: none;
  margin: 4px;
}

/* 项目图片上传器美化 */
.project-uploader-card :deep(.el-upload--picture-card) {
  width: 160px;
  height: 160px;
  border-radius: 24px;
  border: 2px dashed #fcd34d;
  background-color: #fffbeb;
  transition: all 0.3s;
}

.project-uploader-card :deep(.el-upload--picture-card:hover) {
  background-color: #fef3c7;
  border-color: #fbbf24;
}

/* 变动日志时间轴分页 */
.log-timeline-pagination :deep(.el-pager li) {
  border-radius: 10px;
  margin: 0 3px;
  font-weight: 800;
  font-size: 12px;
  color: #94a3b8;
  min-width: 28px;
  height: 28px;
}

.log-timeline-pagination :deep(.el-pager li.is-active) {
  background-color: #4f46e5;
  color: #fff;
}

.log-timeline-pagination :deep(.btn-prev),
.log-timeline-pagination :deep(.btn-next) {
  min-width: 28px;
  height: 28px;
}
</style>
