<template>
    <div class="dress-up-container user-message-page-wrapper">

        <!-- 主内容区域 -->
        <div class="main-content-message">
            <!-- 左侧消息类型导航 -->
            <div class="message-type-card section-card">
                <div class="card-title">
                    <i class="el-icon-menu"></i>
                    消息分类
                </div>
                <div class="message-type-bar">
                    <span
                        :class="['message-type-item', { 'message-type-item--active': messageTypeItem === messageType.type }]"
                        @click="messageTypeSelected(messageType.type)"
                        v-for="(messageType, index) in filteredMessageTypes"
                        :key="index"
                    >
                        {{ messageType.detail }}
                    </span>
                </div>
                <div class="clear-all-messages">
                    <el-button type="info" size="small" @click="clearMessage">
                        <i class="el-icon-delete"></i>
                        全部设为已读
                    </el-button>
                </div>
            </div>

            <!-- 右侧消息列表 -->
            <div class="message-list-section section-card">
                <div class="card-title">
                    <i class="el-icon-document"></i>
                    我的消息
                </div>

                <div v-if="filteredMessageList.length === 0" class="empty-message-list">
                    <el-empty description="暂无消息"></el-empty>
                </div>
                <div v-else class="message-items-list">
                    <div
                        class="message-item-card"
                        :class="{ 'message-item-unread': !message.isRead }"
                        v-for="(message, index) in filteredMessageList"
                        :key="index"
                    >
                        <div class="message-item-header">
                            <span class="message-icon-wrapper">
                                <span v-if="message.messageType === 1 || message.messageType === 2">
                                    <img class="sender-avatar" :src="message.senderAvatar" alt="用户头像">
                                </span>
                                <i v-else-if="message.messageType === 3" class="el-icon-data-analysis"></i>
                                <i v-else-if="message.messageType === 4" class="el-icon-warning-outline"></i>
                                <i v-else class="el-icon-info"></i> <!-- 默认图标 -->
                            </span>
                            <div class="message-content-wrapper">
                                <div class="message-main-info">
                                    <div class="message-sender-name" v-if="message.messageType === 1 || message.messageType === 2">
                                        {{ message.senderName }}
                                    </div>
                                    <div class="message-title-text">
                                        <span v-if="message.messageType === 1">评论了你的文章：{{ commentDeal(message.content)[2] }}</span>
                                        <span v-else-if="message.messageType === 2">点赞了你的评论：{{ message.content }}</span>
                                        <span v-else-if="message.messageType === 3">指标提醒：{{ message.content }}</span>
                                        <span v-else-if="message.messageType === 4">系统通知：{{ message.content }}</span>
                                        <span v-else>{{ message.content }}</span>
                                    </div>
                                </div>
                                <div class="message-actions-time">
                                    <span class="message-time">{{ formatTime(message.createTime) }}</span>
                                    <el-button type="text" size="mini" @click="replyUser(message)" v-if="message.messageType === 1" class="reply-btn">回复</el-button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
// import Logo from "@/components/Logo";
import Swal from "sweetalert2";
import request from '@/utils/request'; // 引入 request 实例

export default {
    // components: { Logo },
    data() {
        return {
            userInfo: {},
            messageQueryDto: {},
            messageList: [],
            messageTypes: [],
            messageTypeItem: null, // 初始化为 null，与“全部消息”的 type 保持一致
            dialogEvaluationsOperation: false,
            message: {}
        };
    },
    computed: {
        filteredMessageTypes() {
            // 确保 "全部消息" 始终是第一个，并且只显示特定类型
            const allTypes = [
                { type: null, detail: "全部消息" },
                { type: 3, detail: "指标提醒" },
                { type: 4, detail: "系统通知" },
                // 可以根据需要添加其他消息类型
            ];
            // 过滤掉不在预设列表中的类型，并合并
            const uniqueTypes = new Map(allTypes.map(item => [item.type, item]));
            this.messageTypes.forEach(type => {
                if (!uniqueTypes.has(type.type) && type.type !== null && type.detail !== "全部消息") { // 避免重复添加全部消息
                    uniqueTypes.set(type.type, type);
                }
            });
            return Array.from(uniqueTypes.values());
        },
        filteredMessageList() {
            // 根据 selectedMessageType 过滤
            if (this.messageTypeItem === null) {
                return this.messageList; // 显示全部消息
            }
            return this.messageList.filter(m => m.messageType === this.messageTypeItem);
        }
    },
    created() {
        this.getUserInfo();
        this.loadAllMessageType(); // 先加载消息类型，再根据类型加载消息
    },
    methods: {
        commentDeal(content) {
            // 假设 content 格式为 "parentId;contentId;评论内容"
            if (content && typeof content === 'string') {
                const parts = content.split(";");
                // 确保至少有三个部分，否则返回原始 content 或一个默认值
                return parts.length >= 3 ? parts : [null, null, content];
            }
            return [null, null, content]; // 返回一个默认结构
        },
        replyUser(message) {
            Swal.fire({
                title: `回复【${message.senderName}】`,
                input: "text",
                inputPlaceholder: "请输入回复内容...",
                showCancelButton: true,
                confirmButtonText: "发送",
                cancelButtonText: "取消",
                customClass: {
                    container: 'my-swal-container', // 自定义类，方便样式覆盖
                    popup: 'my-swal-popup',
                    title: 'my-swal-title',
                    input: 'my-swal-input',
                    confirmButton: 'my-swal-confirm-button',
                    cancelButton: 'my-swal-cancel-button'
                },
                preConfirm: (value) => { // 使用 preConfirm 进行验证和处理
                    if (!value || value.trim() === '') {
                        Swal.showValidationMessage('内容不能为空哦');
                        return false;
                    }
                    return value; // 返回有效值
                }
            }).then(result => {
                if (result.isConfirmed) {
                    const content = result.value;
                    const commentParts = this.commentDeal(message.content);
                    this.saveCommentData(
                        message.senderId,
                        content,
                        commentParts
                    );
                }
            });
        },
        async saveCommentData(senderId, content, commentParts) {
            try {
                const comment = {
                    content,
                    parentId: commentParts[0], // 评论回复父ID
                    contentType: "NEWS", // 假设都是针对新闻的评论
                    contentId: commentParts[1], // 新闻ID
                    replierId: senderId // 被回复的用户ID
                };
                const response = await request.post("/evaluations/insert", comment);
                const { data } = response;
                if (data.code === 200) {
                    this.$notify({
                        title: "评论回复",
                        message: "回复成功",
                        type: "success"
                    });
                    // 可以考虑刷新消息列表或更新相关状态
                } else {
                    this.$notify.error({
                        title: "评论回复",
                        message: data.msg || "回复失败"
                    });
                }
            } catch (error) {
                console.error("回复异常：", error);
                this.$notify.error({
                    title: "评论回复",
                    message: "回复失败，请稍后再试"
                });
            }
        },
        async clearMessage() {
            const confirmed = await Swal.fire({
                title: "清空消息",
                text: "您确定要将所有消息设为已读吗？此操作不可撤销。",
                icon: "warning",
                showCancelButton: true,
                confirmButtonText: "确认",
                cancelButtonText: "取消",
                confirmButtonColor: '#d4af37', // 替换为主题色
                cancelButtonColor: '#909399', // 替换为 Info 色
                customClass: {
                    container: 'my-swal-container',
                    popup: 'my-swal-popup',
                    title: 'my-swal-title',
                    content: 'my-swal-text',
                    confirmButton: 'my-swal-confirm-button',
                    cancelButton: 'my-swal-cancel-button'
                }
            });
            if (confirmed.isConfirmed) {
                try {
                    const response = await request.put("/message/clearMessage");
                    const { data } = response;
                    if (data.code === 200) {
                        this.$notify.success({
                            title: "消息清除",
                            message: "所有消息已设为已读"
                        });
                        this.loadAllUsersMessage(); // 刷新消息列表
                    } else {
                        this.$notify.error({
                            title: "消息清除",
                            message: data.msg || "操作失败"
                        });
                    }
                } catch (error) {
                    console.error("清空消息异常：", error);
                    this.$notify.error({
                        title: "消息清除",
                        message: "操作失败，请稍后再试"
                    });
                }
            }
        },
        messageTypeSelected(messageType) {
            this.messageTypeItem = messageType;
            // 理论上这里应该重新调用后端接口，根据 messageTypeItem 来过滤
            // 但如果后端接口不支持按类型查询，就只能前端过滤
            // 假设 loadAllUsersMessage 会根据 this.messageQueryDto 自动带上 messageType
            this.messageQueryDto.messageType = messageType;
            this.loadAllUsersMessage();
        },
        getUserInfo() {
            const userInfo = sessionStorage.getItem("userInfo");
            if (userInfo) {
                this.userInfo = JSON.parse(userInfo);
            } else {
                // 处理用户未登录的情况，例如跳转到登录页
                // this.$router.push('/login');
            }
        },
        async loadAllMessageType() {
            try {
                const response = await request.get("/message/types");
                const { data } = response;
                if (data.code === 200) {
                    this.messageTypes = data.data || [];
                    // 将“全部消息”作为默认选中项
                    this.messageTypeItem = null; // null 对应“全部消息”
                    this.loadAllUsersMessage(); // 类型加载完后加载所有消息
                }
            } catch (error) {
                console.error("加载消息类型失败：", error);
                this.messageTypes = [];
            }
        },
        async loadAllUsersMessage() {
            if (!this.userInfo || !this.userInfo.id) {
                // console.warn("用户信息未加载，无法获取消息。");
                return;
            }
            try {
                this.messageQueryDto.userId = this.userInfo.id;
                // 如果 messageTypeItem 为 null，则不传 messageType 参数，表示查询全部
                if (this.messageTypeItem !== null) {
                    this.messageQueryDto.messageType = this.messageTypeItem;
                } else {
                    delete this.messageQueryDto.messageType; // 删除该属性
                }
                
                const response = await request.post("/message/query", this.messageQueryDto);
                const { data } = response;
                if (data.code === 200) {
                    this.messageList = data.data || [];
                } else {
                    this.messageList = [];
                }
            } catch (error) {
                console.error("加载用户消息失败：", error);
                this.messageList = [];
            }
        },
        formatTime(time) {
            if (!time) return '';
            return new Date(time).toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            });
        }
    }
};
</script>

<style scoped lang="scss">
// 统一的颜色和阴影变量 (可以放在一个公共 SCSS 文件中，这里为了演示直接包含)
$primary-color: #d4af37; // 金色
$secondary-color: #8b9dc3; // 蓝色
$bg-color-light: #fcfcfa; // 页面背景
$border-color: #e0e0e0; // 边框颜色
$shadow-light: rgba(0,0,0,0.05); // 轻度阴影
$shadow-medium: rgba(0,0,0,0.1); // 中度阴影
$text-color-dark: #2d2d2a;
$text-color-medium: #6a6a66;
$success-color: #67c23a; // Element UI 成功色
$info-color: #909399; // Element UI 信息色

// 从 dress-up-container 复制过来的顶层容器样式
.dress-up-container {
    padding: 30px;
    max-width: 1400px;
    margin: 0 auto;
    background-color: $bg-color-light;
    min-height: calc(100vh - 120px);
    display: flex;
    flex-direction: column;
    gap: 30px;
}

// 页面级别的 Section Header 样式 (与 DressUp 页面保持一致)
.section-header {
    margin-bottom: 20px;
    position: relative;

    .page-title {
        font-size: 28px;
        color: $text-color-dark;
        margin: 0 0 8px 0;
        font-weight: 600;
        letter-spacing: 1px;
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .page-subtitle {
        font-size: 16px;
        color: $text-color-medium;
        margin: 0 0 15px 0;
    }

    .divider {
        height: 3px;
        width: 80px;
        background-color: $primary-color;
        border-radius: 2px;
        transition: width 0.3s ease;

        &:hover {
            width: 120px;
        }
    }
}

// 统一的卡片样式
.section-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px $shadow-light;
    padding: 24px;
    display: flex;
    flex-direction: column;
    flex: 1;
}

.card-title {
    font-size: 20px;
    color: $text-color-dark;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 15px;
    padding-bottom: 10px;
    border-bottom: 1px dashed #eee;

    i {
        color: $primary-color;
    }
}

// 消息中心页面的特定布局
.user-message-page-wrapper {
    // 覆盖原页面的背景色，使用统一的 bg-color-light
    background: $bg-color-light;
    padding: 30px; // 保持与 dress-up-container 一致的 padding
}

.main-content-message {
    display: flex;
    gap: 30px;
    align-items: stretch; // 让两侧高度一致
    flex: 1; // 占据可用空间

    @media (max-width: 992px) {
        flex-direction: column; // 小屏幕下垂直排列
    }
}

/* 消息类型卡片 */
.message-type-card {
    flex: 0 0 250px; // 固定宽度
    gap: 15px; // 内部元素间距

    @media (max-width: 992px) {
        flex: none; // 小屏幕下取消固定宽度
        width: 100%;
    }

    .message-type-bar {
        display: flex;
        flex-direction: column; // 垂直排列
        gap: 10px; // 类型项间距
        padding-left: 0; // 移除原先的 padding
        margin-top: 5px;

        .message-type-item {
            display: block; // 让 span 占据整行
            padding: 10px 15px;
            border-radius: 8px;
            font-size: 16px;
            color: $text-color-dark;
            cursor: pointer;
            transition: all 0.2s ease;
            font-weight: 400; // 默认不加粗

            &:hover {
                background-color: #f5f5f5;
                color: $primary-color;
            }
        }

        .message-type-item--active {
            background-color: #fffaf0; // 选中状态的背景色
            color: $primary-color;
            font-weight: 600; // 选中状态加粗
            position: relative;
            // 可以在这里添加一个左侧指示条
            &::before {
                content: '';
                position: absolute;
                left: 0;
                top: 50%;
                transform: translateY(-50%);
                width: 4px;
                height: 70%;
                background-color: $primary-color;
                border-radius: 2px;
            }
        }
    }

    .clear-all-messages {
        margin-top: auto; // 将按钮推到卡片底部
        padding-top: 15px; // 增加与上方内容的间距
        border-top: 1px dashed #eee;

        .el-button {
            width: 100%;
            // Element UI 按钮样式覆盖
            background-color: $info-color;
            border-color: $info-color;
            color: #fff;
            &:hover {
                background-color: darken($info-color, 8%);
                border-color: darken($info-color, 8%);
            }
        }
    }
}

/* 消息列表区域 */
.message-list-section {
    flex: 3; // 占据大部分空间
    gap: 15px; // 消息项之间的间距

    .empty-message-list {
        padding: 50px 0;
        .el-empty {
            padding: 0;
        }
    }

    .message-items-list {
        display: flex;
        flex-direction: column;
        gap: 15px;
    }

    .message-item-card {
        padding: 18px 24px;
        border-radius: 10px;
        border: 1px solid $border-color;
        background: #fff; // 默认白色背景
        transition: all 0.3s ease;
        cursor: pointer; // 消息项可点击
        box-shadow: 0 2px 8px $shadow-light;

        &:hover {
            transform: translateY(-3px);
            box-shadow: 0 4px 12px $shadow-medium;
        }
    }

    .message-item-unread {
        background: #fffaf0; // 未读消息背景色
        border-color: lighten($primary-color, 20%); // 未读消息边框色
        .message-item-header .message-content-wrapper .message-title-text {
             font-weight: 600; // 未读消息标题加粗
             color: $text-color-dark;
        }
    }

    .message-item-header {
        display: flex;
        align-items: flex-start; // 顶部对齐
        gap: 15px;
    }

    .message-icon-wrapper {
        flex-shrink: 0; // 不压缩图标
        width: 48px;
        height: 48px;
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        background-color: lighten($primary-color, 45%); // 浅金色背景
        border: 2px solid lighten($primary-color, 35%); // 浅金色边框

        .sender-avatar {
            width: 44px; // 比 wrapper 小一些
            height: 44px;
            border-radius: 50%;
            object-fit: cover;
        }

        i {
            font-size: 24px;
            color: $primary-color; // 图标颜色
        }
    }

    .message-content-wrapper {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 5px;
    }

    .message-main-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
    }

    .message-sender-name {
        font-size: 16px;
        font-weight: 600;
        color: $text-color-dark;
    }

    .message-title-text {
        font-size: 14px;
        color: $text-color-medium;
        line-height: 1.5;
    }

    .message-actions-time {
        display: flex;
        align-items: center;
        gap: 15px;
        margin-top: 8px; // 与内容区的间距
    }

    .message-time {
        font-size: 12px;
        color: $text-color-medium;
    }

    .reply-btn {
        color: $primary-color; // 回复按钮使用主题色
        font-size: 12px;
        padding: 0; // 移除默认 Element Text 按钮的 padding
        height: auto;
        &:hover {
            text-decoration: underline;
        }
    }
}


/* Element UI 组件的微调，使其与整体风格更协调 */
::v-deep {
    // 覆盖 Swal 样式
    .my-swal-popup {
        border-radius: 12px !important;
        box-shadow: 0 8px 24px rgba(0,0,0,0.1) !important;
    }
    .my-swal-title {
        color: $text-color-dark !important;
        font-size: 20px !important;
        font-weight: 600 !important;
    }
    .my-swal-text {
        color: $text-color-medium !important;
        font-size: 16px !important;
    }
    .my-swal-input {
        border-color: $border-color !important;
        &:focus {
            border-color: $primary-color !important;
            box-shadow: 0 0 0 2px rgba($primary-color, 0.2) !important;
        }
    }
    .my-swal-confirm-button {
        background-color: $primary-color !important;
        border-color: $primary-color !important;
        &:hover {
            background-color: darken($primary-color, 8%) !important;
            border-color: darken($primary-color, 8%) !important;
        }
    }
    .my-swal-cancel-button {
        background-color: $info-color !important;
        border-color: $info-color !important;
        &:hover {
            background-color: darken($info-color, 8%) !important;
            border-color: darken($info-color, 8%) !important;
        }
    }
}


/* 响应式调整 */
@media (max-width: 992px) {
    .dress-up-container {
        padding: 20px;
        gap: 20px;
    }
    .main-content-message {
        flex-direction: column;
        gap: 20px;
    }
    .message-type-card {
        flex: none;
        width: 100%;
        .message-type-bar {
            flex-direction: row; // 小屏幕下横向排列
            flex-wrap: wrap; // 允许换行
            justify-content: center;
            .message-type-item {
                flex: 1 1 auto; // 自适应宽度
                max-width: 30%; // 限制最大宽度，每行3个左右
                text-align: center;
                &:before { // 小屏幕下移除左侧指示条
                    display: none;
                }
            }
        }
        .clear-all-messages {
            margin-top: 20px;
        }
    }
    .message-list-section {
        flex: none;
        width: 100%;
    }
    .message-item-card {
        padding: 15px 20px;
    }
    .message-icon-wrapper {
        width: 40px;
        height: 40px;
        .sender-avatar {
            width: 36px;
            height: 36px;
        }
        i {
            font-size: 20px;
        }
    }
    .message-sender-name {
        font-size: 15px;
    }
    .message-title-text {
        font-size: 13px;
    }
    .message-time {
        font-size: 11px;
    }
}

@media (max-width: 600px) {
    .dress-up-container {
        padding: 15px;
    }
    .section-header {
        .page-title {
            font-size: 24px;
        }
        .page-subtitle {
            font-size: 14px;
        }
    }
    .card-title {
        font-size: 18px;
    }
    .message-type-card .message-type-bar .message-type-item {
        max-width: 45%; // 每行2个
    }
    .message-item-card {
        padding: 12px 15px;
    }
    .message-item-header {
        gap: 10px;
    }
    .message-icon-wrapper {
        width: 36px;
        height: 36px;
        .sender-avatar {
            width: 32px;
            height: 32px;
        }
        i {
            font-size: 18px;
        }
    }
    .message-sender-name {
        font-size: 14px;
    }
    .message-title-text {
        font-size: 12px;
    }
    .message-actions-time {
        gap: 10px;
    }
}
</style>