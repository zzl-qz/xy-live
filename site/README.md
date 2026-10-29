# XY Live 官方网站

项目介绍页，部署在 GitHub Pages。

## 技术说明

纯静态站点，零构建：

```text
site/
├── index.html          # 单页内容
├── assets/
│   ├── style.css       # 全部样式
│   ├── app.js          # 导航、滚动动画、代码标签页
│   └── logo.svg        # 站点图标
└── .nojekyll           # 关闭 Jekyll 处理
```

不依赖任何 npm 包或 CDN，直接打开 `index.html` 即可预览。

## 本地预览

直接双击 `index.html` 可以看内容，但用本地 HTTP 服务更贴近线上效果：

```bash
# 方式一：Node.js
npx serve site

# 方式二：Python
python -m http.server 8080 --directory site
```

然后访问终端输出的地址（默认 `http://localhost:8080`）。

## 部署

仓库已配置 GitHub Actions 工作流 `.github/workflows/deploy-pages.yml`，
在 `master` 分支推送 `site/` 目录的改动后自动发布。

**首次部署需要手动开启一次 GitHub Pages：**

1. 进入仓库 **Settings → Pages**。
2. 在 **Build and deployment → Source** 选择 **GitHub Actions**。
3. 推送到 `master`，或到 **Actions** 页面手动触发 `Deploy site to GitHub Pages`。
4. 发布地址为 `https://zzl-qz.github.io/xy-live/`。

## 修改内容

| 想改什么 | 改哪里 |
| --- | --- |
| 文案、模块列表、文档链接 | `index.html` |
| 配色、间距、动画 | `assets/style.css` 顶部的 CSS 变量 |
| 交互行为 | `assets/app.js` |
| 架构图 | `index.html` 中 `#architecture` 内的内联 SVG |

配色主变量：

```css
--primary: #8b5cf6;
--primary-2: #d946ef;
--cyan: #22d3ee;
--bg: #07070f;
```

架构图使用内联 SVG 绘制，无外部依赖；修改时保持 `viewBox="0 0 1040 616"` 的比例即可。

## 维护提醒

- 站点中的版本号、模块数量、端口需要与仓库代码保持一致，改动技术栈时请同步更新本页。
- 文档链接指向 GitHub 上的 Markdown 文件，文件名含中文时使用 URL 编码（如 `%E6%A8%A1%E5%9D%97...`）。
