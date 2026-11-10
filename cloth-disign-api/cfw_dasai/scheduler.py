from apscheduler.schedulers.blocking import BlockingScheduler
import os
import subprocess
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('spider-scheduler')

def run_spider():
    """运行爬虫"""
    logger.info("开始运行爬虫...")
    try:
        # 切换到爬虫目录
        os.chdir('d:\\web\\cloth_design\\cloth-disign-api\\cfw_dasai')
        # 运行爬虫
        result = subprocess.run(
            ['python', '-m', 'scrapy', 'crawl', 'news'],
            capture_output=True,
            text=True
        )
        logger.info(f"爬虫运行完成，返回码: {result.returncode}")
        if result.stdout:
            logger.info(f"爬虫输出: {result.stdout}")
        if result.stderr:
            logger.warning(f"爬虫错误: {result.stderr}")
    except Exception as e:
        logger.error(f"运行爬虫时出错: {str(e)}")

if __name__ == "__main__":
    # 创建调度器
    scheduler = BlockingScheduler()
    
    # 设置每天凌晨2点运行
    scheduler.add_job(
        run_spider,
        'cron',
        hour=2,
        minute=0,
        id='cfw_spider',
        replace_existing=True
    )
    
    # 立即运行一次，用于测试
    logger.info("立即运行一次爬虫进行测试...")
    run_spider()
    
    logger.info("调度器启动，将在每天凌晨2点运行爬虫...")
    logger.info("按 Ctrl+C 退出")
    
    try:
        # 启动调度器
        scheduler.start()
    except KeyboardInterrupt:
        logger.info("调度器已停止")
        scheduler.shutdown()
