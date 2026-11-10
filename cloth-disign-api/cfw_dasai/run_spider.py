import os
import subprocess
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    filename='spider.log'
)
logger = logging.getLogger('spider-runner')

def run_spider():
    """运行爬虫"""
    logger.info("开始运行爬虫...")
    try:
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
    run_spider()
