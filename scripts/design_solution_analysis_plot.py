import os
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle

file_range = range(3, 10)

headers = ["|Θ|"] + [str(i) for i in file_range]
data = [headers]

methods = [
    "Enumeration-70%", "Proposed-70%",
    "Enumeration-80%", "Proposed-80%",
    "Enumeration-90%", "Proposed-90%",
    "Enumeration-100%", "Proposed-100%",
    "Graham-100%"
]

rows = {method: ['-'] * len(file_range) for method in methods}


for idx, x in enumerate(file_range):
    filename = f"result/design_solution_analysis/CoresCMP_cond_{x}.txt"
    if not os.path.exists(filename):
        print(f"File not found: {filename}")
        continue

    with open(filename, "r") as f:
        lines = [line.strip().rstrip(',') for line in f if line.strip()]
        values = list(map(float, lines))

        if len(values) < 9:
            print(f"The data in {filename} is less than 9 lines. Skip!")
            continue

        for i, pct in enumerate(["70%", "80%", "90%", "100%"]):
            rows[f"Enumeration-{pct}"][idx] = round(values[i + 4], 2)
            rows[f"Proposed-{pct}"][idx] = round(values[i], 2)

        rows["Graham-100%"][idx] = round(values[-1], 2)

for method in methods:
    data.append([method] + rows[method])

pdf_filename = "result/comparison_table.pdf"
doc = SimpleDocTemplate(pdf_filename, pagesize=landscape(letter))
table = Table(data)

style = TableStyle([
    ('BACKGROUND', (0, 0), (-1, 0), colors.lightgrey),
    ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
    ('FONTNAME', (0, 0), (-1, -1), 'Helvetica'),
    ('ALIGN', (1, 1), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
])
table.setStyle(style)

doc.build([table])

print(f"The table comparing the average number of required cores is generated in {pdf_filename}")
