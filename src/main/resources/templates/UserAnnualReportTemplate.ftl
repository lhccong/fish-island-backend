<#-- 
  用户年度报告 FreeMarker 模板
  说明：所有变量均由 AnnualReportTemplateService 预先计算好，模板仅负责渲染展示层 HTML。
  参考图片样式：卡片式布局，浅蓝到白色渐变背景，关键数字蓝色粗体突出显示
-->
<div style="font-family:'PingFang SC','Microsoft YaHei',sans-serif;background:#e0f2fe;min-height:100vh;box-sizing:border-box;padding:20px 12px;position:relative;overflow-y:auto;">
    <#-- 背景装饰圆形 -->
    <div style="position:absolute;top:-100px;right:-100px;width:300px;height:300px;background:radial-gradient(circle,rgba(34,197,94,0.15),transparent);border-radius:50%;filter:blur(60px);"></div>
    <div style="position:absolute;bottom:-150px;left:-150px;width:400px;height:400px;background:radial-gradient(circle,rgba(59,130,246,0.12),transparent);border-radius:50%;filter:blur(80px);"></div>
    <div style="position:absolute;top:50%;left:10%;width:200px;height:200px;background:radial-gradient(circle,rgba(34,197,94,0.1),transparent);border-radius:50%;filter:blur(50px);"></div>

    <div id="annual-report-poster"
         style="width:750px;max-width:100%;margin:0 auto;position:relative;z-index:1;background:linear-gradient(135deg,#e0f2fe 0%,#f0f9ff 50%,#ffffff 100%);border-radius:24px;padding:28px 28px 32px;box-shadow:0 18px 45px rgba(15,23,42,0.18);">
        <#-- 顶部区域：用户信息和平台标识 -->
        <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:16px;">
            <#-- 左侧：用户头像和昵称 -->
            <div style="display:flex;align-items:center;gap:12px;">
                <#if avatar?? && avatar != "">
                    <img src="${avatar}" alt="avatar"
                         style="width:70px;height:70px;border-radius:50%;object-fit:cover;border:3px solid rgba(59,130,246,0.2);box-shadow:0 2px 8px rgba(0,0,0,0.1);"/>
                <#else>
                    <div style="width:70px;height:70px;border-radius:50%;background:linear-gradient(135deg,#3b82f6,#2563eb);display:flex;align-items:center;justify-content:center;color:#fff;font-size:28px;font-weight:700;border:3px solid rgba(59,130,246,0.2);box-shadow:0 2px 8px rgba(0,0,0,0.1);">${(displayName!"用户")?substring(0,1)}</div>
                </#if>
                <div style="font-size:22px;font-weight:700;color:#1e40af;">${displayName!"用户"}</div>
            </div>

            <#-- 右侧：平台标识 -->
            <div style="display:flex;align-items:center;gap:8px;">
                <img src="https://oss.cqbo.com/moyu/moyu.png" alt="摸鱼岛Logo"
                     style="width:45px;height:45px;object-fit:contain;" onerror="this.style.display='none';"/>
                <div style="font-size:20px;font-weight:600;color:#1e40af;">摸鱼岛</div>
            </div>
        </div>

        <#-- 右上角：二维码卡片（缩小版） -->
        <div style="position:absolute;top:28px;right:28px;background:#ffffff;border-radius:12px;padding:14px;box-shadow:0 4px 12px rgba(0,0,0,0.08);text-align:center;z-index:10;width:140px;">


            <#-- 二维码区域 -->
            <div style="margin:6px 0;">
                <img src="https://oss.cqbo.com/moyu/yucoder.cn.png" alt="年度报告二维码"
                     style="width:110px;height:110px;border:2px solid #e5e7eb;border-radius:6px;"/>
            </div>
            <div style="font-size:11px;color:#64748b;margin-top:4px;line-height:1.3;">
                扫码生成您的 <span style="color:#2563eb;font-weight:700;">
                    <#if year??>
                        ${year?c}
                    <#else>
                        ${.now?string("yyyy")}
                    </#if>
                    年度报告
                </span>
            </div>
        </div>

        <#-- 年度标题横幅：低饱和主题色背景，弱存在感承托“年度报告” -->
        <div style="background:linear-gradient(135deg,rgba(37,99,235,0.06),rgba(45,212,191,0.12));border-radius:12px 999px 999px 12px;padding:8px 18px;margin-bottom:18px;box-shadow:0 2px 8px rgba(15,23,42,0.06);max-width:380px;">
            <div style="font-size:18px;font-weight:700;color:#0f172a;display:flex;align-items:baseline;gap:6px;">
                <#if year??>
                    ${year?c}
                <#else>
                    ${.now?string("yyyy")}
                </#if>
                <span style="color:#2563eb;">年度摸鱼总结</span>
            </div>
        </div>

        <#-- 主标题：年度关键词（大号蓝色粗体，用作年度报告的核心标签） -->
        <div style="font-size:34px;font-weight:800;color:#2563eb;text-align:center;margin-bottom:20px;line-height:1.15;">
            ${annualKeyword!"摸鱼新手"}
        </div>

        <#-- 卡片区域：网格布局，部分卡片整行，部分卡片并排 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:12px;">
            <#-- 卡片一：内容发布统计（整行） -->
            <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;grid-column:1 / -1;">
                <#assign postCountValue = postCount!0>
                <#assign totalWordsValue = totalWords!0>
                <div style="font-size:14px;color:#64748b;margin-bottom:8px;line-height:1.5;">
                    ${(
                        contentSummary!"这一年，您在摸鱼岛留下了珍贵的足迹。"
                    )?replace(
                        r"([0-9]+(?:\.[0-9]+)?)",
                        "<span style=\"color:#2563eb;font-weight:700;font-size:20px;\">$1</span>",
                        "r"
                    )}
                </div>
                <div style="font-size:12px;color:#94a3b8;margin-top:8px;line-height:1.5;">
                    摸到的🐟才是属于自己的，打造属于每个摸鱼人心目中的乌托邦。
                </div>
                <#-- 装饰图标 -->
                <div style="position:absolute;bottom:10px;right:10px;width:60px;height:60px;opacity:0.1;">
                    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
                         style="width:100%;height:100%;">
                        <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="#2563eb" stroke-width="2"/>
                    </svg>
                </div>
            </div>

            <#-- 卡片二：互动统计（左列） -->
            <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;">
                <#assign postThumbsValue = postThumbs!0>
                <#assign postFavoursValue = postFavours!0>
                <#assign emoticonFavoursValue = emoticonFavours!0>
                <div style="font-size:14px;color:#64748b;margin-bottom:8px;line-height:1.5;">
                    <#if postThumbsValue gte 1000>
                        这些内容累计收获了 <span
                            style="color:#2563eb;font-weight:700;font-size:20px;">${(postThumbsValue/1000)?string("0.0")}千</span> 次好评互动。
                    <#elseif postThumbsValue gt 0>
                        这些内容累计收获了 <span
                            style="color:#2563eb;font-weight:700;font-size:20px;">${postThumbsValue}</span> 次好评互动。
                    <#else>
                        这些内容正在等待更多互动。
                    </#if>
                </div>
                <#if postFavoursValue gt 0>
                    <div style="font-size:14px;color:#64748b;margin-top:8px;line-height:1.5;">
                        您也留下了 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${postFavoursValue}</span>
                        条点赞收藏和评论,这对摸鱼岛的伙伴们都很重要。
                    </div>
                </#if>
                <#if emoticonFavoursValue gt 0>
                    <div style="font-size:14px;color:#64748b;margin-top:8px;line-height:1.5;">
                        另外,您还收藏了 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${emoticonFavoursValue}</span>
                        个表情包,快乐加倍。
                    </div>
                </#if>
                <#-- 点赞图标装饰 -->
                <div style="position:absolute;bottom:10px;right:10px;width:50px;height:50px;opacity:0.15;">
                    <svg viewBox="0 0 24 24" fill="#22c55e" xmlns="http://www.w3.org/2000/svg"
                         style="width:100%;height:100%;">
                        <path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/>
                    </svg>
                </div>
            </div>

            <#-- 卡片三：浏览与学习统计（右列） -->
            <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;">
                <div style="font-size:14px;color:#64748b;line-height:1.5;">
                    ${(
                        summaryText!""
                    )?replace(
                        r"([0-9]+(?:\\.[0-9]+)?)",
                        "<span style=\"color:#2563eb;font-weight:700;font-size:20px;\">$1</span>",
                        "r"
                    )}
                </div>
            </div>

            <#-- 卡片四：初次相遇日期（左列） -->
            <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;">
                <#if registerTime??>
                    <div style="font-size:14px;color:#64748b;line-height:1.5;">
                        您与摸鱼岛初次相遇于 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${registerTime?string("yyyy 年 M 月 d 日")}</span>,您的每次进步我们都记得。
                    </div>
                    <#if accompanyDays?? && accompanyDays gt 0>
                        <div style="font-size:14px;color:#64748b;margin-top:6px;line-height:1.5;">
                            从那一天到现在,您已经在摸鱼岛摸鱼了 <span
                                    style="color:#2563eb;font-weight:700;font-size:20px;">${accompanyDays}</span> 天。
                        </div>
                    </#if>
                <#else>
                    <div style="font-size:14px;color:#64748b;line-height:1.5;">
                        感谢您与摸鱼岛一起成长,您的每次进步我们都记得。
                    </div>
                </#if>
            </div>

            <#-- 卡片五：特别事件/最佳帖子（右列，如有） -->
            <#if bestPostTitle?? && bestPostTitle != "">
                <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;">
                    <#if bestPostDate??>
                        <div style="font-size:14px;color:#64748b;margin-bottom:8px;line-height:1.5;">
                            <span style="color:#2563eb;font-weight:700;font-size:20px;">${bestPostDate}</span>您发布了最佳帖子《${bestPostTitle}
                            》,还记得您收藏的那些精彩内容吗?
                        </div>
                    <#else>
                        <div style="font-size:14px;color:#64748b;margin-bottom:8px;line-height:1.5;">
                            您发布了最佳帖子《${bestPostTitle}》,还记得您收藏的那些精彩内容吗?
                        </div>
                    </#if>
                    <div style="font-size:12px;color:#94a3b8;margin-top:8px;line-height:1.5;">
                        不断摸鱼🦑，方能致富。
                    </div>
                </div>
            </#if>

            <#-- 卡片六：宠物养成（左列） -->
            <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;">
                <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
                    <img src="${petSkinUrl!"https://oss.cqbo.com/moyu/moyu.png"}"
                         alt="pet-skin"
                         style="width:120px;height:120px;object-fit:contain;border-radius:12px;border:1px solid #e2e8f0;background:#f8fafc;box-shadow:0 2px 8px rgba(0,0,0,0.05);">
                    <div style="flex:1;min-width:200px;">
                        <#if petCount?? && petCount gt 0>
                            <div style="font-size:14px;color:#64748b;line-height:1.5;">
                                这一年,您养成了 <span style="color:#2563eb;font-weight:700;font-size:18px;">${petCount}</span>
                                只宠物
                                <#if topPetName??>
                                ,<span style="color:#2563eb;font-weight:700;font-size:18px;">${topPetName}</span> 达到了
                                <span style="color:#2563eb;font-weight:700;font-size:18px;">${topPetLevel!0}</span> 级
                                </#if>。
                            </div>
                        <#else>
                            <div style="font-size:14px;color:#64748b;line-height:1.5;">
                                这一年,您还没有领养宠物,期待与您的第一只摸鱼小伙伴相遇。
                            </div>
                        </#if>
                    </div>
                </div>
            </div>

            <#-- 卡片七：赞助支持（右列） -->
            <div style="background:#ffffff;border-radius:14px;padding:20px;box-shadow:0 4px 12px rgba(0,0,0,0.06);position:relative;overflow:hidden;">
                <#assign donationValue = donationText!"0.00">
                <#if donationValue != "0.00">
                    <div style="font-size:14px;color:#64748b;line-height:1.5;">
                        这一年,您为摸鱼岛赞助了 <span
                                style="color:#2563eb;font-weight:700;font-size:20px;">${donationValue}</span>
                        元,谢谢您的真金白银支持。
                    </div>
                <#else>
                    <div style="font-size:14px;color:#64748b;line-height:1.7;">
                        <div style="margin-bottom:8px;">🎉 摸鱼岛祝您新年快乐！ 🎉</div>
                        <div style="margin-bottom:8px;">感谢你这一年抽空上岛偷闲回血 🐟</div>
                        <div style="margin-bottom:8px;">2026 愿你钱包鼓一点、心态松一点、日子好一点。</div>
                        <div style="margin-bottom:10px;">忙里偷闲，累了就回来看看。</div>
                        <div style="text-align:left;">—— 聪 · 摸鱼岛 🏝️🐟</div>
                    </div>
                </#if>
            </div>

            <#-- 底部补位卡片：再次总结发布情况（左下角） -->

            </div>

        </div>
    </div>
</div>


<script src="https://cdn.jsdelivr.net/npm/html2canvas@1.4.1/dist/html2canvas.min.js"></script>
<script>
    function downloadAnnualReportPng() {
        var poster = document.getElementById('annual-report-poster');
        if (!poster || typeof html2canvas === 'undefined') {
            return;
        }
        html2canvas(poster, {
            useCORS: true,
            backgroundColor: null,
            scale: window.devicePixelRatio > 1 ? 2 : 1.5
        }).then(function (canvas) {
            var link = document.createElement('a');
            link.download = 'moyu-annual-report.png';
            link.href = canvas.toDataURL('image/png');
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        });
    }
</script>