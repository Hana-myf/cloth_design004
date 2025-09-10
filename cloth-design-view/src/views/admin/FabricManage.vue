<template>
  <el-row style="background-color: #FFFFFF;padding: 5px 0;border-radius: 5px;">
    <!-- 筛选区域 -->
    <el-row style="padding: 10px;margin-left: 10px;">
      <el-row>
        <!-- 面料类型筛选 -->
        <el-select
          @change="handleTypeChange"
          size="small"
          v-model="fabricQueryDto.type"
          placeholder="面料类型"
        >
          <el-option
            v-for="type in fabricTypes"
            :key="type"
            :label="type"
            :value="type"
          >
          </el-option>
        </el-select>
        
        <!-- 名称搜索 -->
        <el-input
          size="small"
          style="width: 188px;margin-left: 5px;margin-right: 6px;"
          v-model="fabricQueryDto.name"
          placeholder="面料名称"
          clearable
          @clear="handleFilterClear"
        >
          <el-button
            slot="append"
            @click="handleFilter"
            icon="el-icon-search"
          ></el-button>
        </el-input>
        
        <!-- 新增按钮 -->
        <span style="float: right;">
          <el-button
            size="small"
            style="background-color: rgb(96, 98, 102);color: rgb(247,248,249);border: none;"
            class="customer"
            type="info"
            @click="add"
            ><i class="el-icon-plus"></i>新增面料</el-button
          >
        </span>
      </el-row>
    </el-row>

    <!-- 列表区域 -->
    <el-row style="margin: 0 20px;border-top: 1px solid rgb(245,245,245);">
      <el-table
        row-key="id"
        @selection-change="handleSelectionChange"
        :data="tableData"
        style="width: 100%"
      >
        <!-- 面料图片 -->
        <el-table-column prop="imageUrl" width="100" label="面料图片">
          <template slot-scope="scope">
            <img
              :src="scope.row.imageUrl"
              style="width: 60px;height: 40px;border-radius: 5px;object-fit: cover;"
            />
          </template>
        </el-table-column>
        
        <!-- 面料名称 -->
        <el-table-column prop="name" label="面料名称" width="180"></el-table-column>
        
        <!-- 面料类型 -->
        <el-table-column prop="type" label="面料类型" width="120">
          <template slot-scope="scope">
            <span class="type-tag">{{ scope.row.type }}</span>
          </template>
        </el-table-column>
        
        <!-- 创建时间 -->
        <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
        
        <!-- 描述 -->
        <el-table-column prop="description" label="描述">
          <template slot-scope="scope">
            <el-tooltip :content="scope.row.description" placement="top">
              <div class="description-text">{{ scope.row.description }}</div>
            </el-tooltip>
          </template>
        </el-table-column>
        
        <!-- 操作按钮 -->
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <span class="text-button" @click="handleEdit(scope.row)">编辑</span>
            <span class="text-button" @click="handleDelete(scope.row)"
              >删除</span
            >
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页控件 -->
      <el-pagination
        style="margin: 20px 0;"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="totalItems"
      ></el-pagination>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :show-close="false"
      :visible.sync="dialogVisible"
      width="50%"
    >
      <div slot="title">
        <p class="dialog-title">{{ !isEdit ? "新增面料" : "修改面料" }}</p>
      </div>
      
      <div style="padding:0 20px;">
        <!-- 面料图片上传 -->
        <el-row style="margin-top: 10px;">
          <p>*面料图片 <span style="color:red" v-if="imageError">{{ imageError }}</span></p>
          <el-upload
  class="avatar-uploader"
  action="/api/personal-heath/v1.0/file/upload"
  :show-file-list="false"
  :on-success="handleImageUploadSuccess"
  :on-error="handleImageUploadError"
  :before-upload="beforeImageUpload"
 >
            <img
              v-if="formData.imageUrl"
              :src="formData.imageUrl"
              style="height: 180px;width: 240px;object-fit: cover;border-radius: 4px;"
            />
            <div v-else class="upload-placeholder">
              <i class="el-icon-plus avatar-uploader-icon"></i>
              <p>点击上传面料图片</p>
            </div>
          </el-upload>
        </el-row>
        
        <!-- 面料名称 -->
        <el-row style="margin-top: 20px;">
          <p>*面料名称 <span style="color:red" v-if="nameError">{{ nameError }}</span></p>
          <el-input
            v-model="formData.name"
            placeholder="请输入面料名称"
            clearable
          ></el-input>
        </el-row>
        
        <!-- 面料类型 -->
        <el-row style="margin-top: 20px;">
          <p>*面料类型 <span style="color:red" v-if="typeError">{{ typeError }}</span></p>
          <el-select
            v-model="formData.type"
            placeholder="请选择面料类型"
          >
            <el-option
              v-for="type in fabricTypes"
              :key="type"
              :label="type"
              :value="type"
            ></el-option>
          </el-select>
        </el-row>
        
        <!-- 面料描述 -->
        <el-row style="margin-top: 20px;">
          <p>面料描述 <span style="color:#999">(选填)</span></p>
          <el-input
            v-model="formData.description"
            placeholder="请输入面料描述信息"
            type="textarea"
            :rows="4"
          ></el-input>
        </el-row>
      </div>
      
      <span slot="footer" class="dialog-footer">
        <el-button
          size="small"
          v-if="!isEdit"
          style="background-color: rgb(96, 98, 102);color: rgb(247,248,249);border: none;"
          class="customer"
          type="info"
          @click="submitForm"
          >保存</el-button
        >
        <el-button
          size="small"
          v-else
          style="background-color: rgb(96, 98, 102);color: rgb(247,248,249);border: none;"
          class="customer"
          type="info"
          @click="submitForm"
          >更新</el-button
        >
        <el-button
          class="customer"
          size="small"
          style="background-color: rgb(241, 241, 241);border: none;"
          @click="dialogVisible = false"
          >取消</el-button
        >
      </span>
    </el-dialog>
  </el-row>
</template>

<script>
export default {
  data() {
    return {
      // 表格数据
      tableData: [],
      totalItems: 0,
      currentPage: 1,
      pageSize: 10,
      
      // 查询条件
      fabricQueryDto: {
        name: '',
        type: ''
      },
      
      // 面料类型列表（可从接口加载）
      fabricTypes: ['棉', '羊毛', '丝绸', '亚麻', '化纤', '混纺'],
      
      // 弹窗状态
      dialogVisible: false,
      isEdit: false,
      
      // 表单数据
      formData: {
        id: null,
        name: '',
        type: '',
        imageUrl: '',
        description: ''
      },
      
      // 表单验证错误
      imageError: '',
      nameError: '',
      typeError: '',
      
      // 选中的行
      selectedRows: []
    };
  },
  created() {
    this.fetchFabricList();
  },
  methods: {
    // 获取面料列表
    async fetchFabricList() {
      try {
        const params = {
          current: this.currentPage,
          size: this.pageSize,
          name: this.fabricQueryDto.name,
          type: this.fabricQueryDto.type
        };
        const response = await this.$axios.post('/fabric/query', params);
        if (response.data.code === 200) {
          this.tableData = response.data.data.fabricList;
          this.totalItems = response.data.data.totalCount;
        }
      } catch (error) {
        console.error('获取面料列表失败:', error);
        this.$message.error('加载面料数据失败');
      }
    },
    
    // 新增按钮点击
    add() {
      this.isEdit = false;
      this.formData = {
        id: null,
        name: '',
        type: '',
        imageUrl: '',
        description: ''
      };
      this.clearFormErrors();
      this.dialogVisible = true;
    },
    
    // 编辑按钮点击
    handleEdit(row) {
      this.isEdit = true;
      this.formData = { ...row };
      this.clearFormErrors();
      this.dialogVisible = true;
    },
    
    // 删除按钮点击
    async handleDelete(row) {
      const confirmed = await this.$swalConfirm({
        title: '确认删除',
        text: `确定要删除面料"${row.name}"吗？`,
        icon: 'warning'
      });
      if (confirmed) {
        try {
          const response = await this.$axios.post(`/fabric/delete/${row.id}`);
          if (response.data.code === 200) {
            this.$message.success('删除成功');
            this.fetchFabricList();
          } else {
            this.$message.error('删除失败');
          }
        } catch (error) {
          console.error('删除面料失败:', error);
          this.$message.error('删除失败，请重试');
        }
      }
    },
    
    // 表单提交
    async submitForm() {
      if (!this.validateForm()) return;
      
      try {
        let response;
        if (this.isEdit) {
          // 修改操作
          response = await this.$axios.post('/fabric/update', this.formData);
        } else {
          // 新增操作
          response = await this.$axios.post('/fabric/save', this.formData);
        }
        
        if (response.data.code === 200) {
          this.$message.success(this.isEdit ? '更新成功' : '新增成功');
          this.dialogVisible = false;
          this.fetchFabricList();
        } else {
          this.$message.error(response.data.msg || (this.isEdit ? '更新失败' : '新增失败'));
        }
      } catch (error) {
        console.error('提交面料数据失败:', error);
        this.$message.error('操作失败，请重试');
      }
    },
    
    // 表单验证
    validateForm() {
      let isValid = true;
      this.clearFormErrors();
      
      // 验证图片
      if (!this.formData.imageUrl) {
        this.imageError = '请上传面料图片';
        isValid = false;
      }
      
      // 验证名称
      if (!this.formData.name || this.formData.name.trim() === '') {
        this.nameError = '请输入面料名称';
        isValid = false;
      }
      
      // 验证类型
      if (!this.formData.type) {
        this.typeError = '请选择面料类型';
        isValid = false;
      }
      
      return isValid;
    },
    
    // 清空表单错误
    clearFormErrors() {
      this.imageError = '';
      this.nameError = '';
      this.typeError = '';
    },
    
    // 图片上传成功
    handleImageUploadSuccess(res) {
      if (res.code === 200) {
        this.formData.imageUrl = res.data;
        this.imageError = '';
        this.$message.success('图片上传成功');
      } else {
        this.imageError = '图片上传失败，请重试';
      }
    },
    
    // 图片上传失败
    handleImageUploadError() {
      this.imageError = '图片上传失败，请重试';
    },
    
    // 上传前校验
    beforeImageUpload(file) {
      const isImage = file.type.startsWith('image/');
      const isLt2M = file.size / 1024 / 1024 < 2;
      
      if (!isImage) {
        this.$message.error('只能上传图片文件');
        return false;
      }
      if (!isLt2M) {
        this.$message.error('图片大小不能超过2MB');
        return false;
      }
      return true;
    },
    
    // 筛选相关方法
    handleFilter() {
      this.currentPage = 1;
      this.fetchFabricList();
    },
    handleFilterClear() {
      this.fabricQueryDto = { name: '', type: '' };
      this.fetchFabricList();
    },
    handleTypeChange() {
      this.fetchFabricList();
    },
    
    handleSizeChange(val) {
  this.pageSize = val;
  this.currentPage = 1; // 切换每页条数时，重置为第一页
  this.fetchFabricList();
  },
  handleCurrentChange(val) {
  // 确保页码不小于1
  this.currentPage = Math.max(val, 1); 
  this.fetchFabricList();
  },
    // 选择行变化
    handleSelectionChange(selection) {
      this.selectedRows = selection;
    }
  }
};
</script>

<style scoped lang="scss">
/* 类型标签样式 */
.type-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #f0f7ff;
  color: #1890ff;
  font-size: 12px;
}

/* 描述文本样式 */
.description-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
}

/* 上传占位样式 */
.upload-placeholder {
  width: 240px;
  height: 180px;
  border: 1px dashed #ddd;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.upload-placeholder p {
  margin-top: 10px;
  font-size: 14px;
}

/* 复用资讯管理的样式 */
.dialog-footer {
  display: flex;
  justify-content: center;
  align-items: center;
}

.customer {
  margin: 0 8px;
}

.text-button {
  margin: 0 5px;
  color: #409eff;
  cursor: pointer;
}

.text-button:hover {
  text-decoration: underline;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
</style>