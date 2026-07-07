#!/usr/bin/env python3
"""Generate hand-drawn laby-admin architecture draw.io XML (no animation, loose layout)."""

EDGE_MAIN = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;"
    "jettySize=auto;html=1;sketch=1;"
    "strokeColor=#333333;strokeWidth=2;endArrow=block;endFill=1;"
)
EDGE_SOFT = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;"
    "jettySize=auto;html=1;sketch=1;"
    "strokeColor=#6c8ebf;strokeWidth=1;endArrow=classic;endFill=1;"
)
EDGE_DATA = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;"
    "jettySize=auto;html=1;sketch=1;"
    "strokeColor=#82b366;strokeWidth=1;endArrow=classic;endFill=1;"
)
EDGE_EXT = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;"
    "jettySize=auto;html=1;sketch=1;"
    "strokeColor=#d79b00;strokeWidth=1;endArrow=classic;endFill=1;"
)
EDGE_RAG = (
    "edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;"
    "jettySize=auto;html=1;sketch=1;"
    "strokeColor=#b85450;strokeWidth=1;dashed=1;dashPattern=8 4;"
    "endArrow=classic;endFill=1;"
)

S_BOX = (
    "rounded=1;whiteSpace=wrap;html=1;sketch=1;"
    "fillColor={fill};strokeColor={stroke};fontSize=13;fontColor=#333333;"
    "spacing=8;arcSize=8;"
)
S_GROUP = (
    "rounded=1;whiteSpace=wrap;html=1;sketch=1;dashed=1;dashPattern=8 8;"
    "fillColor={fill};strokeColor={stroke};verticalAlign=top;fontStyle=1;"
    "fontSize=15;fontColor=#333333;align=center;spacingTop=14;arcSize=10;"
)
S_TITLE = (
    "text;html=1;strokeColor=none;fillColor=none;align=center;"
    "verticalAlign=middle;sketch=1;fontStyle=1;fontSize=28;fontColor=#333333;"
)
S_NOTE = (
    "text;html=1;strokeColor=none;fillColor=none;align=center;"
    "verticalAlign=middle;sketch=1;fontSize=13;fontColor=#888888;"
)


def cell(cid, value, style, x, y, w, h, parent="1"):
    return (
        f'<mxCell id="{cid}" value="{value}" style="{style}" vertex="1" parent="{parent}">'
        f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>'
    )


def edge(eid, style, source, target, points=None, label="", lx=0):
    pts = ""
    if points:
        inner = "".join(f'<mxPoint x="{x}" y="{y}"/>' for x, y in points)
        pts = f"<Array as=\"points\">{inner}</Array>"
    lbl = ""
    if label:
        lbl = (
            f'<mxCell id="{eid}l" value="{label}" style="edgeLabel;html=1;'
            f'align=center;verticalAlign=middle;resizable=0;points=[];sketch=1;'
            f'fontSize=12;background=#FFF9F0;" vertex="1" connectable="0" parent="{eid}">'
            f'<mxGeometry x="{lx}" relative="1" as="geometry">'
            f'<mxPoint as="offset"/></mxGeometry></mxCell>'
        )
    return (
        f'<mxCell id="{eid}" style="{style}" edge="1" parent="1" '
        f'source="{source}" target="{target}">'
        f'<mxGeometry relative="1" as="geometry">{pts}</mxGeometry>{lbl}</mxCell>'
    )


def system_page():
    w, h = 1680, 1220
    cx = 840
    parts = [
        cell("bg", "", "rounded=0;sketch=1;fillColor=#FFF9F0;strokeColor=none;", 0, 0, w, h),
        cell("title", "laby-admin 系统架构", S_TITLE, 440, 28, 800, 44),
        cell("sub", "企业法务合同 AI 审核平台", S_NOTE, 490, 78, 700, 24),
        # ① users
        cell("g1", "① 业务用户", S_GROUP.format(fill="#fff2cc", stroke="#d6b656"), 100, 120, 1480, 130),
        cell("u1", "法务专员&#xa;合同审核", S_BOX.format(fill="#fff2cc", stroke="#d6b656"), 160, 168, 380, 62),
        cell("u2", "审批人&#xa;BPM 审批", S_BOX.format(fill="#fff2cc", stroke="#d6b656"), 650, 168, 380, 62),
        cell("u3", "管理员&#xa;权限配置", S_BOX.format(fill="#fff2cc", stroke="#d6b656"), 1140, 168, 380, 62),
        # ② frontend  (gap 60)
        cell("g2", "② 前端", S_GROUP.format(fill="#dae8fc", stroke="#6c8ebf"), 220, 310, 1240, 110),
        cell("fe", "web-ele&#xa;Vue3 · Vben · Element Plus · 合同 / 知识库 / BPM / AI", S_BOX.format(fill="#dae8fc", stroke="#6c8ebf"), 280, 352, 1120, 52),
        # ③ gateway
        cell("g3", "③ 接入层", S_GROUP.format(fill="#fff2cc", stroke="#d6b656"), 220, 480, 1240, 110),
        cell("srv", "laby-server&#xa;Spring Boot 3.5 · REST / SSE · OAuth2 · 多租户", S_BOX.format(fill="#fff2cc", stroke="#d6b656"), 280, 522, 1120, 52),
        # ④ modules
        cell("g4", "④ 业务模块", S_GROUP.format(fill="#e1d5e7", stroke="#9673a6"), 100, 650, 1480, 130),
        cell("m1", "legal ★&#xa;Pipeline · Agent", S_BOX.format(fill="#f8cecc", stroke="#b85450") + "fontStyle=1;", 140, 692, 240, 62),
        cell("m2", "ai&#xa;RAG · AgentScope", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 420, 692, 240, 62),
        cell("m3", "bpm&#xa;Flowable", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 700, 692, 220, 62),
        cell("m4", "system&#xa;用户权限", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 960, 692, 220, 62),
        cell("m5", "infra&#xa;文件 · 任务", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 1220, 692, 240, 62),
        # ⑤ bottom — gap 80 from modules
        cell("g5a", "⑤ 数据存储", S_GROUP.format(fill="#d5e8d4", stroke="#82b366"), 100, 920, 720, 140),
        cell("d1", "MySQL&#xa;业务 · FULLTEXT", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 130, 968, 140, 68),
        cell("d2", "Redis&#xa;Stream", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 310, 968, 140, 68),
        cell("d3", "Qdrant&#xa;向量 Dense", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 490, 968, 140, 68),
        cell("d4", "MinIO&#xa;对象存储", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 670, 968, 140, 68),
        cell("g5b", "⑤ 外部服务", S_GROUP.format(fill="#ffe6cc", stroke="#d79b00"), 860, 920, 720, 140),
        cell("s1", "LLM API&#xa;Chat · Embed", S_BOX.format(fill="#ffe6cc", stroke="#d79b00"), 890, 968, 180, 68),
        cell("s2", "OnlyOffice&#xa;文档预览", S_BOX.format(fill="#ffe6cc", stroke="#d79b00"), 1100, 968, 180, 68),
        cell("s3", "Flowable&#xa;工作流", S_BOX.format(fill="#ffe6cc", stroke="#d79b00"), 1310, 968, 180, 68),
        cell("foot", "手绘风格 · 自上而下主链路 · 侧向连线分列通道", S_NOTE + "align=right;", 980, 1100, 660, 24),
        # main chain — center column only
        edge("e1", EDGE_MAIN, "u2", "fe", label="HTTPS", lx=-0.1),
        edge("e2", EDGE_MAIN, "fe", "srv", label="REST / SSE", lx=-0.1),
        edge("e3", EDGE_MAIN, "srv", "m1", [(cx, 630), (260, 630)], "路由", lx=0.1),
        # module → data: corridor y=860–890 (80px gap below modules)
        edge("e4", EDGE_DATA, "m1", "d1", [(260, 860), (200, 860)]),
        edge("e5", EDGE_DATA, "m1", "d2", [(260, 875), (380, 875)]),
        edge("e6", EDGE_DATA, "m2", "d3", [(540, 860), (560, 860)]),
        edge("e7", EDGE_DATA, "m5", "d4", [(1340, 860), (740, 860)]),
        # module → external: separate lanes y=885–905
        edge("e8", EDGE_EXT, "m2", "s1", [(540, 890), (980, 890)]),
        edge("e9", EDGE_EXT, "m3", "s3", [(810, 905), (1400, 905)]),
        edge("e10", EDGE_EXT, "m4", "s2", [(1070, 885), (1190, 885)]),
    ]
    root = '<mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(parts)
    return (
        f'<diagram id="sys-v3" name="系统架构">'
        f'<mxGraphModel dx="{w}" dy="{h}" grid="1" gridSize="10" guides="1" '
        f'pageWidth="{w}" pageHeight="{h}" background="#FFF9F0">'
        f"<root>{root}</root></mxGraphModel></diagram>"
    )


def tech_page():
    w, h = 1680, 1180
    parts = [
        cell("tbg", "", "rounded=0;sketch=1;fillColor=#FFF9F0;strokeColor=none;", 0, 0, w, h),
        cell("tt", "laby-admin 技术架构 · N-Tier L5 → L1", S_TITLE, 380, 28, 920, 44),
        cell("ts", "分层泳道 · 主链居中 · RAG 独立右侧", S_NOTE, 480, 78, 720, 24),
        # stacked layers — 70px gap between tiers
        cell("L5", "L5 表现层", S_GROUP.format(fill="#dae8fc", stroke="#6c8ebf"), 120, 120, 980, 110),
        cell("L5d", "web-ele · Vue3 Vben · 合同审核 UI", S_BOX.format(fill="#dae8fc", stroke="#6c8ebf"), 180, 162, 860, 52),
        cell("L4", "L4 接入层", S_GROUP.format(fill="#fff2cc", stroke="#d6b656"), 120, 300, 980, 110),
        cell("L4a", "laby-server · REST / SSE", S_BOX.format(fill="#fff2cc", stroke="#d6b656"), 180, 342, 400, 52),
        cell("L4b", "Security · OAuth2 · Tenant", S_BOX.format(fill="#fff2cc", stroke="#d6b656"), 620, 342, 420, 52),
        cell("L3", "L3 业务层", S_GROUP.format(fill="#e1d5e7", stroke="#9673a6"), 120, 480, 980, 130),
        cell("L3a", "legal ★", S_BOX.format(fill="#f8cecc", stroke="#b85450") + "fontStyle=1;", 160, 528, 150, 62),
        cell("L3b", "ai", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 340, 528, 130, 62),
        cell("L3c", "bpm", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 500, 528, 130, 62),
        cell("L3d", "system", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 660, 528, 130, 62),
        cell("L3e", "infra", S_BOX.format(fill="#e1d5e7", stroke="#9673a6"), 820, 528, 130, 62),
        cell("L2", "L2 框架层", S_GROUP.format(fill="#ffe6cc", stroke="#d79b00"), 120, 680, 980, 110),
        cell("L2a", "laby-framework", S_BOX.format(fill="#ffe6cc", stroke="#d79b00"), 160, 722, 260, 52),
        cell("L2b", "AgentScope 2", S_BOX.format(fill="#ffe6cc", stroke="#d79b00"), 460, 722, 260, 52),
        cell("L2c", "Flowable", S_BOX.format(fill="#ffe6cc", stroke="#d79b00"), 760, 722, 260, 52),
        cell("L1", "L1 基础设施", S_GROUP.format(fill="#d5e8d4", stroke="#82b366"), 120, 860, 980, 120),
        cell("L1a", "MySQL", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 150, 908, 120, 52),
        cell("L1b", "Redis", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 300, 908, 120, 52),
        cell("L1c", "Qdrant", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 450, 908, 120, 52),
        cell("L1d", "MinIO", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 600, 908, 120, 52),
        cell("L1e", "LLM", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 750, 908, 120, 52),
        cell("L1f", "OnlyOffice", S_BOX.format(fill="#d5e8d4", stroke="#82b366"), 900, 908, 120, 52),
        # RAG — far right, no overlap with center
        cell("rag", "RAG Pipeline", S_GROUP.format(fill="#f8cecc", stroke="#b85450"), 1180, 480, 420, 500),
        cell(
            "ragd",
            "Dense (Qdrant)&#xa;+ Sparse (MySQL FULLTEXT)&#xa;↓&#xa;RRF → Rerank&#xa;↓&#xa;legal → Redis Stream",
            S_BOX.format(fill="#f8cecc", stroke="#b85450"),
            1220, 560, 340, 360,
        ),
        cell("tfoot", "主链：web-ele → server → legal → framework → MySQL", S_NOTE + "align=right;", 900, 1040, 740, 24),
        # main chain — single spine x=110 through layer centers (legal path)
        edge("t1", EDGE_MAIN, "L5d", "L4a"),
        edge("t2", EDGE_MAIN, "L4a", "L3a"),
        edge("t3", EDGE_MAIN, "L3a", "L2a"),
        edge("t4", EDGE_MAIN, "L2a", "L1a"),
        # RAG: 2 edges via far-right corridor x=1140
        edge("tr1", EDGE_RAG, "L3b", "ragd", [(1160, 559), (1160, 580)]),
        edge("tr2", EDGE_RAG, "ragd", "L1c", [(1580, 1020), (510, 1020)]),
    ]
    root = '<mxCell id="0"/><mxCell id="1" parent="0"/>' + "".join(parts)
    return (
        f'<diagram id="tech-v3" name="技术架构 N-Tier">'
        f'<mxGraphModel dx="{w}" dy="{h}" grid="1" gridSize="10" guides="1" '
        f'pageWidth="{w}" pageHeight="{h}" background="#FFF9F0">'
        f"<root>{root}</root></mxGraphModel></diagram>"
    )


def main():
    xml = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<mxfile host="app.diagrams.net">'
        + system_page()
        + tech_page()
        + "</mxfile>"
    )
    base = __import__("pathlib").Path(__file__).parent
    (base / "_generated-architecture.xml").write_text(xml, encoding="utf-8")
    (base / "laby-admin-architecture.drawio").write_text(
        xml.replace('<?xml version="1.0" encoding="UTF-8"?>', ""), encoding="utf-8"
    )
    print("ok", len(xml))


if __name__ == "__main__":
    main()
