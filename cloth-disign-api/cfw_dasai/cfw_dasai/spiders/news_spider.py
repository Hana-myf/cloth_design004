import scrapy
from cfw_dasai.items import NewsItem
from datetime import datetime

class NewsSpider(scrapy.Spider):
    name = 'news'
    allowed_domains = ['dasai.cfw.cn']
    start_urls = ['https://dasai.cfw.cn/ds/list0600-1.html']
    
    def __init__(self, *args, **kwargs):
        super(NewsSpider, self).__init__(*args, **kwargs)
        self.max_depth = 3  # 限制爬虫深度
        self.today = datetime.now()
    
    def parse(self, response):
        # 检查深度
        depth = response.meta.get('depth', 0)
        if depth > self.max_depth:
            return
        
        self.logger.info(f'爬取页面: {response.url} (深度: {depth})')
        self.logger.info(f'页面状态: {response.status}')
        
        # 提取赛事列表
        competitions = response.xpath('//ul[contains(@class, "competition")]/li')
        self.logger.info(f'找到 {len(competitions)} 个赛事')
        
        for competition in competitions:
            item = NewsItem()
            
            # 提取标题
            title = competition.xpath('.//a[contains(@class, "name")]/text()').get()
            if title:
                item['name'] = title.strip()
                self.logger.info(f'标题: {item["name"]}')
            
            # 提取封面图片
            cover = competition.xpath('.//a[contains(@class, "pic")]/img/@src').get()
            if cover:
                item['cover'] = response.urljoin(cover)
                self.logger.info(f'封面图片: {item["cover"]}')
            
            # 提取链接
            url = competition.xpath('.//a[contains(@class, "name")]/@href').get()
            if url:
                detail_url = response.urljoin(url)
                self.logger.info(f'详情链接: {detail_url}')
                # 继续爬取详情页面
                yield scrapy.Request(detail_url, callback=self.parse_detail, meta={'item': item})
        
        # 提取下一页链接
        next_page = response.xpath('//div[@class="m-page"]/ul/li/a[contains(@title, "下一页")]/@href').get()
        if next_page:
            next_url = response.urljoin(next_page)
            self.logger.info(f'下一页链接: {next_url}')
            yield scrapy.Request(next_url, callback=self.parse, meta={'depth': depth + 1})
    
    def parse_detail(self, response):
        try:
            item = response.meta.get('item')
            if not item:
                self.logger.error('Item not found in meta')
                return
            
            # 提取时间范围
            time_range = response.xpath('//p[contains(@class, "c6")]/text()').get()
            if time_range:
                time_range = time_range.strip()
                self.logger.info(f'比赛时间: {time_range}')
                
                # 提取详细内容
                content_parts = []
                content_parts.append(f'比赛时间: {time_range}')
                content_parts.append(f'详情链接: {response.url}')
                
                # 提取所有内容段落，保持段落结构
                content_div = response.xpath('//div[contains(@class, "content")]')
                if content_div:
                    # 提取所有段落元素
                    paragraphs = content_div.xpath('.//p | .//div | .//h3 | .//h4 | .//ul | .//ol')
                    if paragraphs:
                        content_text = []
                        for para in paragraphs:
                            # 提取段落文本
                            para_text = para.xpath('.//text()').getall()
                            if para_text:
                                clean_text = ' '.join([t.strip() for t in para_text if t.strip()])
                                if clean_text:
                                    content_text.append(clean_text)
                        # 用换行符连接段落，保持结构
                        if content_text:
                            content_parts.append(f'\n详细内容:\n' + '\n\n'.join(content_text))
                
                # 组合内容
                item['content'] = '\n'.join(content_parts)
                
                # 检查比赛是否未截止
                if self.is_not_ended(time_range):
                    # 设置其他字段
                    item['tagId'] = 12
                    item['readerIds'] = ''
                    item['isTop'] = False
                    item['createTime'] = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                    
                    yield item
                else:
                    self.logger.info(f'比赛已截止: {item.get("name", "Unknown")}')
        except Exception as e:
            self.logger.error(f'解析详情页出错: {e}')
    
    def is_not_ended(self, time_range):
        """检查比赛是否未截止"""
        try:
            # 提取截止日期
            if '-' in time_range:
                parts = time_range.split('-')
                start_date_str = parts[0].strip()
                end_date_str = parts[-1].strip()
                
                # 解析开始日期，获取年份
                if len(start_date_str) >= 4:
                    start_year = start_date_str.split('.')[0]
                else:
                    start_year = str(self.today.year)
                
                # 处理截止日期
                if len(end_date_str) == 5:  # 格式为 MM.dd
                    end_date_str = f'{start_year}.{end_date_str}'
                elif len(end_date_str) == 10:  # 格式为 YYYY.MM.dd
                    pass
                else:
                    return False
                
                # 解析日期
                end_date = datetime.strptime(end_date_str, '%Y.%m.%d')
                # 检查是否未截止
                return end_date >= self.today
        except Exception as e:
            self.logger.error(f'日期解析错误: {e}')
        return False
