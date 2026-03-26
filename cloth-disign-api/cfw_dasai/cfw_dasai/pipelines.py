# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html

# useful for handling different item types with a single interface
from itemadapter import ItemAdapter
import pymysql

class NewsPipeline:
    def __init__(self):
        # 连接数据库
        self.conn = pymysql.connect(
            host='localhost',
            user='root',
            password='root',
            database='personal_health',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
        self.cursor = self.conn.cursor()
    
    def process_item(self, item, spider):
        # 检查是否已存在相同的赛事
        check_sql = "SELECT id, cover FROM news WHERE name = %s"
        self.cursor.execute(check_sql, (item.get('name', ''),))
        existing = self.cursor.fetchone()
        
        # 提取字段值
        name = item.get('name', '')
        content = item.get('content', '')
        tag_id = item.get('tagId', 12)
        cover = item.get('cover', '')
        reader_ids = item.get('readerIds', '')
        is_top = item.get('isTop', False)
        create_time = item.get('createTime', None)
        
        if existing:
            # 无论cover是否为空，都更新记录
            update_sql = """
            UPDATE news SET 
                content = %s, 
                tag_id = %s, 
                cover = %s, 
                reader_ids = %s, 
                is_top = %s, 
                create_time = %s 
            WHERE id = %s
            """
            values = (content, tag_id, cover, reader_ids, is_top, create_time, existing['id'])
            self.cursor.execute(update_sql, values)
            self.conn.commit()
            print(f"更新数据: {name}, 封面: {cover}")
        else:
            # 插入新数据
            insert_sql = """
            INSERT INTO news (
                name, content, tag_id, cover, reader_ids, is_top, create_time
            ) VALUES (%s, %s, %s, %s, %s, %s, %s)
            """
            values = (name, content, tag_id, cover, reader_ids, is_top, create_time)
            self.cursor.execute(insert_sql, values)
            self.conn.commit()
            print(f"插入数据: {name}, 封面: {cover}")
        
        return item
    
    def close_spider(self, spider):
        # 关闭数据库连接
        self.cursor.close()
        self.conn.close()
