<template>
  <el-row style="background-color: #FFFFFF;padding: 5px 0;border-radius: 5px;">
    <el-row style="padding: 10px;margin-left: 10px;">
      <el-row>
         <el-select
          @change="changeNewsTag"
          size="small"
          v-model="newsQueryDto.tagId"
          placeholder="资讯分类"
        >
          <el-option
            v-for="tag in tagsList"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          >
          </el-option>
        </el-select>
        <el-date-picker
          size="small"
          style="margin-left: 5px;width: 220px;"
          v-model="searchTime"
          type="daterange"
          range-separator="至"
          start-placeholder="发布开始"
          end-placeholder="发布结束"
        >
        </el-date-picker>
        <el-input
          size="small"
          style="width: 188px;margin-left: 5px;margin-right: 6px;"
          v-model="newsQueryDto.name"
          placeholder="资讯标题"
          clearable
          @clear="handleFilterClear"
        >
          <el-button
            slot="append"
            @click="handleFilter"
            icon="el-icon-search"
          ></el-button>
        </el-input>
        <span style="float: right;">
          <el-button
            size="small"
            style="background-color: rgb(96, 98, 102);color: rgb(247,248,249);border: none;"
            class="customer"
            type="info"
            @click="add()"
            ><i class="el-icon-plus"></i>新增资讯</el-button
          >
        </span>
      </el-row>
    </el-row>
    <el-row style="margin: 0 20px;border-top: 1px solid rgb(245,245,245);">
      <el-table
        row-key="id"
        @selection-change="handleSelectionChange"
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column prop="cover" width="80" label="首图">
          <template slot-scope="scope">
            <img
              :src="scope.row.cover"
              style="width: 48px;height: 34px;border-radius: 5px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="tagName" width="138" label="所属分类">
          <template slot-scope="scope">
            <span
              ><i class="el-icon-discount" style="margin-right: 3px;"></i>
              {{ scope.row.tagName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="isTop" width="128" label="是否推荐">
          <template slot-scope="scope">
            <i
              v-if="!scope.row.isTop"
              style="margin-right: 5px;"
              class="el-icon-warning"
            ></i>
            <i
              v-else
              style="margin-right: 5px;color: rgb(253, 199, 50);"
              class="el-icon-success"
            ></i>
            <el-tooltip
              v-if="!scope.row.isTop"
              class="item"
              effect="dark"
              content="未推荐资讯，不做推荐展示"
              placement="bottom-end"
            >
              <span
                style="cursor: pointer;;text-decoration: underline;text-decoration-style: dashed;"
                >未推荐</span
              >
            </el-tooltip>
            <span v-else>已推荐</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          width="168"
          label="发布时间"
        ></el-table-column>
        <el-table-column prop="name" label="标题"></el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <span class="text-button" @click="handleEdit(scope.row)">编辑</span>
            <span class="text-button" @click="handleDelete(scope.row)"
              >删除</span
            >
          </template>
        </el-table-column>
      </el-table>
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
    <el-dialog
      :show-close="false"
      :visible.sync="dialogUserOperaion"
      width="50%"
    >
      <div slot="title">
        <p class="dialog-title">{{ !isOperation ? "新增资讯" : "修改资讯" }}</p>
      </div>
      <div style="padding:0 20px;">
        <!-- 封面 -->
        <el-row style="margin-top: 10px;">
          <p>*封面 <span style="color:red" v-if="coverError">{{ coverError }}</span></p>
          <el-upload
            class="avatar-uploader"
            action="/api/personal-heath/v1.0/file/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :on-error="handleAvatarError"
          >
            <img
              v-if="data.cover"
              :src="data.cover"
              style="height: 120px;width: 188px;"
            />
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-row>
        <!-- 标题 -->
        <el-row>
          <p>*标题 <span style="color:red" v-if="titleError">{{ titleError }}</span></p>
          <input
            style="font-size: 32px;line-height: 45px;"
            class="dialog-input"
            v-model="data.name"
            placeholder="标题"
          />
        </el-row>
        <!-- 标签 -->
        <el-row style="margin: 12px 0;">
          <el-row>
            <span class="dialog-hover">所属分类 <span style="color:red" v-if="tagError">{{ tagError }}</span></span>
          </el-row>
          <el-radio-group style="margin-top: 10px;" v-model="data.tagId">
            <el-radio
              :key="index"
              :label="tag.id"
              v-for="(tag, index) in tagsList"
              >{{ tag.name }}</el-radio
            >
          </el-radio-group>
        </el-row>
        <!-- 推荐 -->
        <el-row style="margin: 12px 0;">
          <el-row>
            <span class="dialog-hover">是否推荐</span>
          </el-row>
          <el-switch
            style="user-select: none;padding: 0 6px;"
            v-model="data.isTop"
            active-color="#13ce66"
            inactive-color="rgb(226, 226, 226)"
          >
          </el-switch>
        </el-row>
        <el-row>
          <p>*正文描述 <span style="color:red" v-if="contentError">{{ contentError }}</span></p>
          <Editor
            height="calc(100vh - 500px)"
            :receiveContent="data.content"
            @on-receive="onReceiveContent"
          />
        </el-row>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button
          size="small"
          v-if="!isOperation"
          style="background-color: rgb(96, 98, 102);color: rgb(247,248,249);border: none;"
          class="customer"
          type="info"
          @click="addOperation()"
          >新增</el-button
        >
        <el-button
          size="small"
          v-else
          style="background-color: rgb(96, 98, 102);color: rgb(247,248,249);border: none;"
          class="customer"
          type="info"
          @click="updateOperation()"
          >修改</el-button
        >
        <el-button
          class="customer"
          size="small"
          style="background-color: rgb(241, 241, 241);border: none;"
          @click="dialogUserOperaion = false"
          >取消</el-button
        >
      </span>
    </el-dialog>
  </el-row>
</template>

<script>
import Editor from "@/components/Editor";
export default {
  components: { Editor },
  data() {
    return {
      userPwd: "",
      data: { cover: "", isTop: false }, // 初始化默认值
      filterText: "",
      currentPage: 1,
      pageSize: 10,
      totalItems: 0,
      dialogUserOperaion: false,
      isOperation: false, // false=新增，true=修改
      tableData: [],
      searchTime: [],
      selectedRows: [],
      status: null,
      newsQueryDto: {},
      messsageContent: "",
      tagsList: [],
      // 表单验证错误提示
      coverError: "",
      titleError: "",
      tagError: "",
      contentError: ""
    };
  },
  created() {
    this.fetchFreshData();
    this.loadAllTags();
  },
  methods: {
    changeNewsTag(tagId) {
      this.newsQueryDto.tagId = tagId;
      this.fetchFreshData();
    },
    onReceiveContent(html) {
      this.data.content = html;
    },
    loadAllTags() {
      this.$axios.post(`/tags/query`, {}).then(response => {
        const { data } = response;
        if (data.code === 200) {
          this.tagsList = data.data;
          this.tagsList.unshift({ name: "全部", id: null });
        }
      });
    },
    handleAvatarSuccess(res, file) {
      this.coverError = ""; // 清空错误提示
      if (res.code !== 200) {
        this.$message.error(`资讯封面上传异常`);
        return;
      }
      this.data.cover = res.data;
      this.$message.success(`资讯封面上传成功`);
    },
    handleAvatarError() {
      this.coverError = "封面上传失败，请重新上传";
    },
    handleSelectionChange(selection) {
      this.selectedRows = selection;
    },
    async batchDelete() {
      if (!this.selectedRows.length) {
        this.$message(`未选中任何数据`);
        return;
      }
      const confirmed = await this.$swalConfirm({
        title: "删除资讯数据",
        text: `删除后不可恢复，是否继续？`,
        icon: "warning"
      });
      if (confirmed) {
        try {
          let ids = this.selectedRows.map(entity => entity.id);
          const response = await this.$axios.post(`/news/batchDelete`, ids);
          if (response.data.code === 200) {
            this.$swal.fire({
              title: "删除提示",
              text: response.data.msg,
              icon: "success",
              showConfirmButton: false,
              timer: 2000
            });
            this.fetchFreshData();
            return;
          }
        } catch (e) {
          this.$swal.fire({
            title: "错误提示",
            text: "删除失败",
            icon: "error",
            showConfirmButton: false,
            timer: 2000
          });
          console.error(`资讯信息删除异常：`, e);
        }
      }
    },
    resetQueryCondition() {
      this.newsQueryDto = {};
      this.searchTime = [];
      this.fetchFreshData();
    },
    async updateOperation() {
      // 验证表单
      if (!this.validateForm()) return;

      try {
        const response = await this.$axios.put("/news/update", this.data);
        this.$swal.fire({
          title: "资讯信息修改",
          text: response.data.msg,
          icon: response.data.code === 200 ? "success" : "error",
          showConfirmButton: false,
          timer: 1000
        });
        if (response.data.code === 200) {
          this.closeDialog();
          this.fetchFreshData();
          this.clearFormData();
        }
      } catch (error) {
        console.error("提交表单时出错:", error);
        this.$message.error("提交失败，请稍后再试！");
      }
    },
    async addOperation() {
      // 验证表单必填项
      if (!this.validateForm()) return;

      try {
        const response = await this.$axios.post("/news/save", this.data);
        this.$message[response.data.code === 200 ? "success" : "error"](
          response.data.msg
        );
        if (response.data.code === 200) {
          this.closeDialog();
          this.fetchFreshData();
          this.clearFormData();
        }
      } catch (error) {
        console.error("提交表单时出错:", error);
        this.$message.error("提交失败，请稍后再试！");
      }
    },
    // 表单验证方法
    validateForm() {
      let isValid = true;
      // 验证封面
      if (!this.data.cover) {
        this.coverError = "请上传封面图片";
        isValid = false;
      } else {
        this.coverError = "";
      }
      // 验证标题
      if (!this.data.name || this.data.name.trim() === "") {
        this.titleError = "请输入资讯标题";
        isValid = false;
      } else {
        this.titleError = "";
      }
      // 验证分类
      if (this.data.tagId === undefined || this.data.tagId === null) {
        this.tagError = "请选择所属分类";
        isValid = false;
      } else {
        this.tagError = "";
      }
      // 验证正文
      if (!this.data.content || this.data.content.trim() === "") {
        this.contentError = "请输入资讯正文";
        isValid = false;
      } else {
        this.contentError = "";
      }
      return isValid;
    },
    closeDialog() {
      this.dialogUserOperaion = false;
    },
    clearFormData() {
      this.data = { cover: "", isTop: false };
      // 清空错误提示
      this.coverError = "";
      this.titleError = "";
      this.tagError = "";
      this.contentError = "";
    },
    async fetchFreshData() {
      try {
        let startTime = null;
        let endTime = null;
        if (this.searchTime && this.searchTime.length === 2) {
          const [startDate, endDate] = this.searchTime.map(date => date.toISOString());
          startTime = `${startDate.split("T")[0]}T00:00:00`;
          endTime = `${endDate.split("T")[0]}T23:59:59`;
        }
        const params = {
          current: this.currentPage,
          size: this.pageSize,
          startTime: startTime,
          endTime: endTime,
          ...this.newsQueryDto
        };
        const response = await this.$axios.post("/news/query", params);
        const { data } = response;
        this.tableData = data.data;
        this.totalItems = data.total;
      } catch (error) {
        console.error("查询资讯信息异常:", error);
      }
    },
    // 新增按钮点击事件（关键修复）
    add() {
      this.dialogUserOperaion = true;
      this.isOperation = false; // 强制设为新增模式
      this.clearFormData(); // 清空表单数据
      // 默认选中第一个可用分类（排除"全部"）
      if (this.tagsList.length > 1) {
        this.data.tagId = this.tagsList[1].id;
      }
    },
    handleFilter() {
      this.currentPage = 1;
      this.fetchFreshData();
    },
    handleFilterClear() {
      this.filterText = "";
      this.handleFilter();
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchFreshData();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.fetchFreshData();
    },
    handleEdit(row) {
      this.dialogUserOperaion = true;
      this.isOperation = true; // 设为修改模式
      // 深拷贝行数据，避免直接修改表格数据
      this.data = { ...row };
      // 清空错误提示
      this.coverError = "";
      this.titleError = "";
      this.tagError = "";
      this.contentError = "";
    },
    handleDelete(row) {
      this.selectedRows = [row]; // 单次删除选中当前行
      this.batchDelete();
    }
  }
};
</script>
<style scoped lang="scss">
.tag-tip {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 5px;
  background-color: rgb(245, 245, 245);
  color: rgb(104, 118, 130);
}

.input-def {
  height: 40px;
  line-height: 40px;
  outline: none;
  border: none;
  font-size: 20px;
  color: rgb(102, 102, 102);
  font-weight: 900;
  width: 100%;
}

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

.dialog-input {
  width: 100%;
  padding: 8px 0;
  border: none;
  border-bottom: 1px solid #eaeaea;
  outline: none;
  transition: border-color 0.3s;
}

.dialog-input:focus {
  border-bottom-color: #409eff;
}
</style>