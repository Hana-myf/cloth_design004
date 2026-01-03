import requests
from bs4 import BeautifulSoup

# 目标网站
url = "https://ssr1.scrape.center/"

# 模拟浏览器请求头（必须加，否则可能被拒）
headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}

try:
    # 1. 发送请求
    response = requests.get(url, headers=headers)
    response.raise_for_status()  # 若请求失败（如404/500），直接抛错
    soup = BeautifulSoup(response.text, "html.parser")

    # 2. 定位所有电影卡片（从你的结构看，每部电影在 <div class="el-card"> 里）
    movie_cards = soup.find_all("div", class_="el-card")  # 核心：找到所有电影容器

    if not movie_cards:
        print("❌ 未找到任何电影卡片，请检查选择器是否正确")
    else:
        print(f"✅ 找到 {len(movie_cards)} 部电影\n")

        # 3. 逐个解析电影卡片中的数据
        for idx, card in enumerate(movie_cards, 1):
            # 👉 电影标题（h2标签，class="m-b-sm"）
            title_tag = card.find("h2", class_="m-b-sm")
            title = title_tag.get_text(strip=True) if title_tag else "标题未找到"

            # 👉 电影类型（div.categories 下的所有span标签）
            categories_tag = card.find("div", class_="categories")
            categories = []
            if categories_tag:
                categories = [span.get_text(strip=True) for span in categories_tag.find_all("span")]
            categories = " | ".join(categories) if categories else "类型未找到"

            # 👉 地区/时长 + 上映时间（div.info 下的两个p标签）
            info_div = card.find("div", class_="info")
            area_duration = "地区/时长未找到"
            release_time = "上映时间未找到"
            if info_div:
                info_ps = info_div.find_all("p")  # 提取info下所有p标签
                if len(info_ps) >= 1:
                    area_duration = info_ps[0].get_text(strip=True)  # 第1个p：地区/时长
                if len(info_ps) >= 2:
                    release_time = info_ps[1].get_text(strip=True)    # 第2个p：上映时间

            # 👉 评分（p标签，class="score"）
            score_tag = card.find("p", class_="score")
            score = score_tag.get_text(strip=True) if score_tag else "评分未找到"

            # 👉 封面图（img标签，class="cover"，取src属性）
            cover_tag = card.find("img", class_="cover")
            cover_url = ""
            if cover_tag and "src" in cover_tag.attrs:  # 确保src属性存在
                cover_url = cover_tag["src"]
                # 补全相对路径（如果src是//开头，加https:）
                if cover_url.startswith("//"):
                    cover_url = "https:" + cover_url
            cover_url = cover_url if cover_url else "封面图未找到"

            # 4. 打印结果
            print(f"🎬 电影 {idx}")
            print(f"标题：{title}")
            print(f"类型：{categories}")
            print(f"地区/时长：{area_duration}")
            print(f"上映时间：{release_time}")
            print(f"评分：{score}")
            print(f"封面图：{cover_url}\n")

except requests.exceptions.RequestException as e:
    print(f"❌ 请求失败：{e}")  # 捕获网络错误（如超时、404等）
except Exception as e:
    print(f"❌ 解析失败：{e}")  # 捕获其他解析错误（如标签不存在）