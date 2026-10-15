import json
from pathlib import Path
from graphify.build import build_from_json
from graphify.cluster import cluster, score_all
from graphify.analyze import god_nodes, surprising_connections, suggest_questions
from graphify.report import generate
from graphify.export import to_json, to_html

extraction = json.loads(Path('graphify-out/.graphify_extract.json').read_text(encoding='utf-8'))
detection  = json.loads(Path('graphify-out/.graphify_detect.json').read_text(encoding='utf-8'))

G = build_from_json(extraction)
communities = cluster(G)
cohesion = score_all(G, communities)
tokens = {'input': 0, 'output': 0}
gods = god_nodes(G)
surprises = surprising_connections(G, communities)

# Generate intelligent community labels based on member labels
labels = {}
for cid, members in communities.items():
    member_labels = [G.nodes[m].get('label', m) for m in members if m in G.nodes]
    # Pick representative keywords or top node name
    if member_labels:
        top_name = member_labels[0]
        # Clean top name
        labels[cid] = f"Community {cid}: {top_name[:30]}"
    else:
        labels[cid] = f"Community {cid}"

questions = suggest_questions(G, communities, labels)

report = generate(G, communities, cohesion, labels, gods, surprises, detection, tokens, '.', suggested_questions=questions)
Path('graphify-out/GRAPH_REPORT.md').write_text(report, encoding='utf-8')
to_json(G, communities, 'graphify-out/graph.json')
to_html(G, communities, 'graphify-out/graph.html', community_labels=labels)

print(f"Graph built successfully:")
print(f"  Nodes: {G.number_of_nodes()}")
print(f"  Edges: {G.number_of_edges()}")
print(f"  Communities: {len(communities)}")
print(f"  Outputs generated:")
print(f"    - graphify-out/GRAPH_REPORT.md")
print(f"    - graphify-out/graph.json")
print(f"    - graphify-out/graph.html")
