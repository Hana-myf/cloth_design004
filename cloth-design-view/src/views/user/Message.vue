<template>
    <div class="user-message-page">
        <div style="padding: 4px 50px;">
            <el-card class="banner-card message-banner-card">
                <div class="message-header-row">
                    <span class="main-title">消息中心</span>
                    <el-button class="plain-black-btn clear-message-btn" @click="clearMessage">
                        <i class="el-icon-s-open"></i> 清空
                    </el-button>
                </div>
            </el-card>
            <div>
                <div class="message-type-bar">
                    <span
                        :class="['message-type', { 'message-type--active': messageTypeItem === messageType.type }]"
                        @click="messageTypeSelected(messageType.type)"
                        v-for="(messageType, index) in filteredMessageTypes"
                        :key="index"
                    >
                        {{ messageType.detail }}
                    </span>
                </div>
            </div>
            <div style="padding: 30px 0;">
                <el-card class="message-list-card">
                    <div v-if="filteredMessageList.length === 0">
                        <el-empty description="暂无消息"></el-empty>
                    </div>
                    <div v-else>
                        <div
                            class="message-item-card"
                            :class="{ 'message-item-unread': !message.isRead }"
                            v-for="(message, index) in filteredMessageList"
                            :key="index"
                        >
                            <el-row class="message-item-row">
                             <div>
                                <span class="bell-icon">
                                    <span v-if="message.messageType === 1 || message.messageType === 2">
                                        <img style="width: 40px;height: 40px;border-radius: 20px;"
                                            :src="message.senderAvatar" alt="" srcset="">
                                    </span>
                                    <i v-if="message.messageType === 3" class="el-icon-data-analysis"></i>
                                </span>
                              </div>
                              <div>
                                <div>
                                    <div v-if="message.messageType === 1 || message.messageType === 2">
                                        <div style="font-size: 18px;">
                                            {{ message.senderName }}
                                        </div>
                                    </div>
                                    <div style="font-size: 18px;">
                                        <span class="message-content" v-if="message.messageType === 1"
                                            style="font-size: 14px;">{{ commentDeal(message.content)[2] }}</span>
                                        <span class="message-content" v-else-if="message.messageType === 3" style="font-size: 14px;">{{ message.content
                                            }}</span>
                                        <span class="message-content" v-else style="font-size: 14px;">点赞了你的评论【{{ message.content
                                            }}】</span>
                                    </div>
                                </div>
                                <div>
                                    <span class="message-time">{{ message.createTime }}</span>
                                    <span @click="replyUser(message)" v-if="message.messageType === 1"
                                        style="font-size: 10px;margin-left: 10px;cursor: pointer;">回复</span>
                                </div>
                              </div>
                            </el-row>
                        </div>
                    </div>
                </el-card>
            </div>
        </div>
    </div>
</template>

<script>
// import Logo from "@/components/Logo";
import Swal from "sweetalert2";

export default {
    // components: { Logo },
    data() {
        return {
            userInfo: {},
            messageQueryDto: {},
            messageList: [],
            messageTypes: [],
            messageTypeItem: {},
            dialogEvaluationsOperation: false,
            message: {}
        };
    },
    computed: {
        filteredMessageTypes() {
            return this.messageTypes.filter(
                (type) =>
                    type.detail === "全部消息" ||
                    type.detail === "指标提醒" ||
                    type.detail === "系统通知"
            );
        },
        filteredMessageList() {
            return this.messageList.filter(
                (m) => m.messageType === 3 || m.messageType === 4
            );
        }
    },
    created() {
        // 1. 拿到当前用户的信息
        this.getUserInfo();
        // 2. 加载用户全部消息
        this.loadAllUsersMessage();
        // 3. 加载全部的消息类型
        this.loadAllMessageType();
    },
    methods: {
        commentDeal(content) {
            return content.split(";");
        },
        // 回复用户
        replyUser(message) {
            Swal.fire({
                title: `回复【${message.senderName}】`,
                input: "text",
                inputPlaceholder: "回复内容",
                showCancelButton: true,
                confirmButtonText: "提交",
                cancelButtonText: "取消",
                inputValidator: value => {
                    if (!value) {
                        return "内容不能为空哦";
                    } else {
                        this.saveCommentData(
                            message.senderId,
                            value,
                            this.commentDeal(message.content)
                        );
                    }
                }
            }).then(result => {
                console.log("操作结果：", result);
            });
        },
        // 保存回复数据
        saveCommentData(senderId, content, ary) {
            const comment = {
                id: 1,
                content,
                parentId: ary[0],
                contentType: "NEWS",
                contentId: ary[1],
                replierId: senderId
            };
            this.$axios
                .post("/evaluations/insert", comment)
                .then(response => {
                    const { data } = response;
                    if (data.code === 200) {
                        this.$notify({
                            title: "评论回复",
                            message: "回复成功",
                            type: "success"
                        });
                    }
                })
                .catch(error => {
                    console.log("回复异常：", error);
                });
        },
        // 将全部的消息设置未已读
        async clearMessage() {
            const confirmed = await this.$swalConfirm({
                title: "消息清除",
                text: `是否将全部的消息设置为已读？`,
                icon: "warning"
            });
            if (confirmed) {
                this.$axios.put("/message/clearMessage").then(response => {
                    const { data } = response;
                    if (data.code === 200) {
                        this.loadAllUsersMessage();
                    }
                });
            }
        },
        evaluationsPut() {
            this.$axios.put("/message/clearMessage").then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.loadAllUsersMessage();
                }
            });
        },
        // 做回复的
        replyEvalustions(message) {
            this.message = message;
            this.dialogEvaluationsOperation = true;
        },
        // 消息类型选中
        messageTypeSelected(messageType) {
            this.messageTypeItem = messageType;
            this.messageQueryDto.messageType = messageType;
            // 如果是评论数据，需要额外处理
            this.loadAllUsersMessage();
        },
        getUserInfo() {
            const userInfo = sessionStorage.getItem("userInfo");
            this.userInfo = JSON.parse(userInfo);
        },
        // 加载全部的消息类型
        loadAllMessageType() {
            this.$axios.get("/message/types").then(response => {
                const { data } = response;
                if (data.code === 200) {
                    this.messageTypes = data.data;
                    const messageType = { type: null, detail: "全部消息" };
                    this.messageTypes.unshift(messageType);
                    this.messageTypes.map(entity => (entity.isCheck = false));
                    this.messageTypeSelected(this.messageTypes[0].type);
                }
            });
        },
        loadAllUsersMessage() {
            const userInfo = sessionStorage.getItem("userInfo");
            const entity = JSON.parse(userInfo);
            this.messageQueryDto.userId = entity.id;
            this.$axios
                .post("/message/query", this.messageQueryDto)
                .then(response => {
                    const { data } = response;
                    if (data.code === 200) {
                        this.messageList = data.data;
                    }
                });
        }
    }
};
</script>

<style scoped lang="scss">
.user-message-page {
    background: linear-gradient(120deg, #e6f9ec 0%, #fafdff 100%);
    min-height: 100vh;
    padding: 32px 0 40px 0;
}
.header-divider {
    height: 4px;
    background: #e6f9ec;
    margin: 0 0 32px 0;
    width: 100%;
}
.banner-card.message-banner-card {
    background: linear-gradient(120deg, #e6f9ec 0%, #fafdff 100%);
    border: none;
    box-shadow: 0 2px 8px rgba(43, 121, 203, 0.08);
    margin-bottom: 18px;
    border-radius: 12px;
    padding: 18px 32px 12px 32px;
}
.main-title {
    font-size: 1.5rem;
    font-weight: 700;
    color: #2bcba8;
}
.user-info-bar {
    float: right;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
}
.user-avatar {
    width: 30px;
    height: 30px;
    border-radius: 15px;
}
.user-name {
    margin-left: 8px;
    font-size: 1.1rem;
    color: #333;
}
.message-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.plain-black-btn.clear-message-btn {
    background: #fff !important;
    color: #222 !important;
    border-radius: 18px !important;
    border: 1px solid #e0e0e0 !important;
    font-weight: 500;
    transition: background 0.2s;
    font-size: 1rem;
    padding: 6px 18px;
}
.plain-black-btn.clear-message-btn:hover {
    background: #f5f5f5 !important;
    color: #111 !important;
}
.message-type-bar {
    display: flex;
    gap: 24px;
    margin: 18px 0 0 0;
    padding-left: 8px;
}
.message-type {
    display: inline-block;
    border-bottom: 4px solid transparent;
    padding-bottom: 6px;
    font-size: 1.1rem;
    color: #333;
    cursor: pointer;
    font-weight: 500;
    transition: border 0.2s, color 0.2s;
}
.message-type--active {
    border-bottom: 4px solid #2bcba8;
    color: #2bcba8;
}
.message-list-card {
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(43, 121, 203, 0.06);
    background: #fff;
    border: none;
    padding: 24px 32px 18px 32px;
}
.message-item-card {
    margin-bottom: 12px;
    padding: 18px 24px;
    border-radius: 10px;
    border: 1px solid #e0e0e0;
    background: #fafafa;
    transition: background 0.2s;
}
.message-item-unread {
    background: #e6f9ec;
}
.message-item-row {
    display: flex;
    align-items: center;
    gap: 18px;
}
.message-item-content {
    flex: 1;
}
.message-item-title {
    font-size: 1.1rem;
    font-weight: 600;
    color: #222;
    margin-bottom: 6px;
}
.bell-icon {
    display: inline-block;
    height: 50px;
    width: 50px;
    border-radius: 25px;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: rgb(106, 190, 238);
    border: 3px solid rgb(92, 172, 219);

    i {
        line-height: 30px;
        width: 30px;
        text-align: center;
        font-size: 25px;
        color: #f1f1f1;
    }
}

.message-time {
    font-size: 12px;
    color: rgb(131, 104, 102);
}

.clear-message {
    display: inline-block;
    margin-left: 10px;
    padding: 6px 12px;
    border-radius: 5px;
}

.clear-message:hover {
    background-color: #f1f1f1;
}

.news-tags {
    display: inline-block;
    padding: 2px 5px;
    background-color: rgb(222, 243, 251);
    color: #1d3cc4;
    font-size: 14px;
    border-radius: 3px;
}
</style>