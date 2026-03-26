<template>
    <div>
        <ul class="nav">
            <li>
                <Logo />
            </li>
            <!-- 功能菜单 -->
            <li v-if="!item.isHidden" 
                :style="{
                    fontSize: selectedIndex === index ? '18px' : '14px',
                    color: selectedIndex === index ? '#1c1c1c' : 'rgb(102 102 102)'
                }" 
                class="funItem" 
                v-for="(item, index) in menus" 
                :key="index" 
                @click="menuClick(`${item.path}`, index)">
                <span>
                    <i :class="item.icon"></i>
                    <span>&nbsp; {{ item.name }}</span>
                </span>
            </li>
            <!-- 搜索框 -->
            <li class="search-item">
                <el-row>
                    <el-col :span="14">
                        <input class="search-input" placeholder="搜索..." @keyup.enter="search" v-model="filterText" />
                    </el-col>
                    <el-col :span="4">
                        <span @click="search" class="search-btn">
                            搜索
                        </span>
                    </el-col>
                </el-row>
            </li>

            <!-- 消息中心 -->
            <li class="message-item">
                <el-badge style="margin-left: 5px;font-size: 16px;" v-if="noReadMsg !== 0" :value="noReadMsg">
                    <span class="message-span" @click="messageCenter">
                        <i class="el-icon-bell"></i>
                    </span>
                </el-badge>
                <span style="margin-left: 5px;font-size: 16px;" v-else class="message-span" @click="messageCenter">
                    <i class="el-icon-bell"></i>
                </span>
            </li>
            <!-- 用户信息 -->
            <li class="user-item">
                <span class="user-block">
                    <el-dropdown class="user-dropdown">
                        <span class="el-dropdown-link" style="display: flex; align-items: center;">
                            <el-avatar :size="35" :src="userInfo.url" style="margin-top: 0;"></el-avatar>
                            <span class="userName" style="margin-left: 5px;font-size: 16px;">{{ userInfo.name }}</span>
                            <i class="el-icon-arrow-down el-icon--right" style="margin-left: 5px;"></i>
                        </span>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item icon="el-icon-user" @click.native="userCenterPanel">个人中心</el-dropdown-item>
                            <el-dropdown-item icon="el-icon-warning-outline" @click.native="resetPwd">修改密码</el-dropdown-item>
                            <el-dropdown-item icon="el-icon-back" @click.native="loginOut">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </span>
            </li>
        </ul>
    </div>
</template>
<script>
import { clearToken } from "@/utils/storage.js";
import Logo from '@/components/Logo.vue';
export default {
    name: "UserMenu",
    components: {
        Logo
    },
    data() {
        return {
            selectedIndex: 0,
            messagePath: '/message',
            loginPath: '/login',
            sysName: '纤云速织',
            defaultPath: '/news-record',
            filterText: '',
            noReadMsg: 0,
        }
    },
    props: {
        // 路由菜单数据
        menus: {
            type: Array,
            required: true
        },
        // 用户信息数据
        userInfo: {
            type: Object,
            required: true
        }
    },
    mounted() {
        this.pathToDo(this.defaultPath);
        this.loadMsgCount();
    },
    methods: {
        // 搜索关键词，返回父组件处理
        search() {
            // 如果当前是搜索页面了，更新关键词即可
            if (this.$route.path === '/search-detail') {
                sessionStorage.setItem('keyWord', this.filterText);
                return;
            }
            // 将关键词存起来
            sessionStorage.setItem('keyWord', this.filterText);
            this.$emit('eventListener', 'search-detail');
        },
        // 个人中心，传回父组件处理
        userCenterPanel() {
            this.$emit('eventListener', 'center');
        },
        // 重置密码，传回父组件处理
        resetPwd() {
            this.$emit('eventListener', 'resetPwd');
        },
        // 退出登录，传回父组件处理
        loginOut() {
            this.$emit('eventListener', 'loginOut');
        },
        // 记录饮食，传回父组件处理
        dietRecord() {
            this.$emit('eventListener', 'dietRecord');
        },
        // 记录个人健康指标，传回父组件处理
        healthDataRecord() {
            this.$emit('eventListener', 'healthDataRecord');
        },
        // 退出登录，传回父组件处理
        loginOut() {
            this.$emit('eventListener', 'loginOut');
        },
        async loadMsgCount() {
            const userInfo = sessionStorage.getItem('userInfo');
            const userInfoEntity = JSON.parse(userInfo);
            const messageQueryDto = { userId: userInfoEntity.id, isRead: false }
            const response = await this.$axios.post(`/message/query`, messageQueryDto);
            const { data } = response;
            if(data.code === 200){
                this.noReadMsg = data.data.length;
            }
        },
        // 不是存量路由，则跳转
        pathToDo(path) {
            if (this.$route.path !== path) {
                this.$router.push(path);
            }
        },
        //路由跳转
        menuClick(path, index) {
            this.selectedIndex = index;
            this.pathToDo(path);
        },
        // 消息中心
        messageCenter() {
            this.selectedIndex = null;
            this.pathToDo(this.messagePath);
        },
        // 退出登录
        async out() {
            const confirmed = await this.$swalConfirm({
                title: '是否退出登录',
                text: `退出后将重新登录，才能使用系统功能`,
                icon: 'warning',
            });
            if (confirmed) {
                this.$swal.fire({
                    title: '退出登录',
                    text: '您已成功退出登录。',
                    icon: 'success', // 使用'success'图标表示操作成功
                    showConfirmButton: false, // 隐藏确认按钮，使得弹窗只展示信息后自动关闭
                    timer: 1300, // 自动关闭弹窗的延迟时间，这里是2秒
                });
                setTimeout(() => {
                    clearToken();
                    this.$router.push('/loginPath');
                }, 1300)
            } else {
                console.log('用户取消了退出操作');
            }
        },
    }
}
</script>
<style scoped lang="scss">
.nav {
    padding: 12px 20px; /* 调整整体内边距，避免右侧内容溢出 */
    height: 70px;
    line-height: 70px;
    list-style: none;
    border-bottom: 1px solid #f1f1f1;
    margin: 0;
    display: flex; /* 使用flex布局更易控制间距 */
    align-items: center;
    justify-content: flex-start;

    li {
        float: left;
        height: 70px;
        line-height: 70px;
        font-weight: 400;
        padding: 0 15px; /* 统一基础间距 */
        user-select: none;
        color: rgb(102, 102, 102);
        font-size: 14px;
        transition: all 0.5s;

        i {
            color: rgb(102, 102, 102);
        }
    }
}

/* 搜索框容器样式 - 作为按钮定位的父容器 */
.search-item {
    margin-left: 3px;
    padding: 0 10px;
    min-width: 300px;
    position: relative; /* 关键：让按钮基于此容器定位 */
}

/* 搜索输入框样式 - 预留按钮空间 */
.search-input {
    outline: none;
    width: 100%;
    font-size: 14px;
    height: 35px;
    line-height: 35px;
    /* 右侧预留按钮+边距空间（按钮宽约60px+左右边距10px=70px） */
    padding: 0 70px 0 15px; 
    border-radius: 5px;
    transition: all 0.3s;
    border: 1px solid #ccc;
    box-sizing: border-box; /* 确保padding不撑开输入框 */
}

/* 搜索按钮样式 - 内嵌在输入框内右侧 */
.search-btn {
    /* 绝对定位实现内嵌效果 */
    position: absolute;
    right: 15px; /* 距离容器右侧的间距（与输入框左侧padding呼应） */
    top: 50%;
    transform: translateY(-50%); /* 垂直居中对齐 */
    
    /* 按钮尺寸小于搜索框 */
    height: 28px; /* 比输入框矮7px */
    line-height: 28px; /* 文字垂直居中 */
    min-width: 50px; /* 按钮最小宽度，小于输入框宽度 */
    width: auto; /* 取消100%宽度，按内容自适应 */
    
    background-color: #000;
    color: #f1f1f1;
    border-radius: 3px; /* 圆角比输入框小，更精致 */
    padding: 0 12px; /* 水平内边距 */
    text-align: center;
    cursor: pointer;
    font-size: 13px; /* 字体稍小，适配按钮尺寸 */
    border: none; /* 去掉默认边框 */
    box-shadow: 0 1px 2px rgba(0,0,0,0.1); /* 轻微阴影，突出按钮 */
}

/* 交互效果优化 */
.search-btn:hover {
    background-color: #333; /* 悬停时加深颜色 */
}

.search-input:focus {
    border-color: #666; /* 输入框聚焦时边框变色 */
    box-shadow: 0 0 0 2px rgba(0,0,0,0.05); /* 轻微聚焦效果 */
}

/* 指标记录样式 */
.health-record-item {
    padding: 0 15px;
    margin-left: 10px; /* 与搜索框保持距离 */
}

.health-record-text {
    cursor: pointer;
    padding: 5px 8px;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover {
        background-color: #f5f5f5;
    }
}

/* 消息中心样式 */
.message-item {
    padding: 0 15px;
    margin-left: 5px;
}

.message-span {
    padding: 5px;
    border-radius: 5px;
    cursor: pointer;

    &:hover {
        background-color: rgb(240, 240, 240);
    }

    i {
        font-size: 16px;
    }
}

/* 用户信息样式 */
.user-item {
    padding: 0 20px;
    margin-left: 10px;
    position: relative;
}

.user-block {
    .userName {
        display: inline-block;
        vertical-align: middle;
        font-size: 14px;
        cursor: pointer;
        user-select: none;
    }
}

/* 修复搜索框聚焦样式拼写错误 */
.search-input:focus {
    border: 1px solid rgb(188, 229, 247);
    outline: none;
}
</style>