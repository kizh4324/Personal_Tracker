import json
from pathlib import Path
import re

# Load detection & AST results
detect = json.loads(Path('graphify-out/.graphify_detect.json').read_text(encoding='utf-8'))
ast_data = json.loads(Path('graphify-out/.graphify_ast.json').read_text(encoding='utf-8'))

doc_files = list(detect.get('files', {}).get('document', []))

# Ensure key planning and tracking files are included
extra_targets = [
    Path('_bmad-output/implementation-artifacts/sprint-status.yaml'),
    Path('ARCHITECTURE-SPINE.md'),
    Path('AGENTS.md'),
    Path('_bmad-output/project-context.md'),
    Path('_bmad-output/planning-artifacts/epics.md')
]
for extra in extra_targets:
    if extra.exists():
        abs_str = str(extra.resolve())
        if abs_str not in doc_files and str(extra) not in doc_files:
            doc_files.append(str(extra))

sem_nodes = []
sem_edges = []
seen_nodes = set()

def normalize_id(text):
    clean = re.sub(r'[^a-zA-Z0-9_]', '_', str(text).lower())
    clean = re.sub(r'_+', '_', clean).strip('_')
    return clean

for filepath_str in doc_files:
    p = Path(filepath_str)
    if not p.exists():
        continue
    rel_path = str(p.relative_to(Path('.'))) if p.is_relative_to(Path('.')) else str(p)
    stem = normalize_id(p.stem)
    
    # Node for the document itself
    doc_id = f"doc_{stem}"
    if doc_id not in seen_nodes:
        sem_nodes.append({
            "id": doc_id,
            "label": p.name,
            "file_type": "document",
            "source_file": rel_path,
            "source_location": None,
            "source_url": None,
            "captured_at": None,
            "author": None,
            "contributor": None
        })
        seen_nodes.add(doc_id)
        
    try:
        content = p.read_text(encoding='utf-8', errors='ignore')
    except Exception:
        continue
        
    lines = content.splitlines()
    prev_section_id = doc_id

    # Handle YAML files like sprint-status.yaml
    if p.suffix.lower() in ('.yaml', '.yml'):
        in_dev_status = False
        for line in lines:
            line_str = line.strip()
            if line_str.startswith('#'):
                comment_text = line_str.lstrip('#').strip()
                if len(comment_text) >= 3:
                    section_stem = normalize_id(comment_text)[:40]
                    section_id = f"{stem}_{section_stem}"
                    if section_id not in seen_nodes:
                        sem_nodes.append({
                            "id": section_id,
                            "label": comment_text,
                            "file_type": "document",
                            "source_file": rel_path,
                            "source_location": None,
                            "source_url": None,
                            "captured_at": None,
                            "author": None,
                            "contributor": None
                        })
                        seen_nodes.add(section_id)
                        sem_edges.append({
                            "source": doc_id,
                            "target": section_id,
                            "relation": "contains_section",
                            "confidence": "EXTRACTED",
                            "confidence_score": 1.0,
                            "source_file": rel_path,
                            "source_location": None,
                            "weight": 1.0
                        })
            elif line_str.startswith('development_status:'):
                in_dev_status = True
            elif in_dev_status and line_str and not line_str.startswith('#') and ':' in line_str:
                parts = line_str.split(':', 1)
                item_key = parts[0].strip()
                item_status = parts[1].strip()
                node_id = f"{stem}_{normalize_id(item_key)}"
                if node_id not in seen_nodes:
                    sem_nodes.append({
                        "id": node_id,
                        "label": f"{item_key}: {item_status}",
                        "file_type": "document",
                        "source_file": rel_path,
                        "source_location": None,
                        "source_url": None,
                        "captured_at": None,
                        "author": None,
                        "contributor": None
                    })
                    seen_nodes.add(node_id)
                    sem_edges.append({
                        "source": doc_id,
                        "target": node_id,
                        "relation": "tracks_status",
                        "confidence": "EXTRACTED",
                        "confidence_score": 1.0,
                        "source_file": rel_path,
                        "source_location": None,
                        "weight": 1.0
                    })
        continue

    # Extract Markdown Headers / Sections as sub-concepts
    for line in lines:
        line_str = line.strip()
        if line_str.startswith('#'):
            header_text = line_str.lstrip('#').strip()
            if not header_text or len(header_text) < 3:
                continue
            section_stem = normalize_id(header_text)[:40]
            section_id = f"{stem}_{section_stem}"
            
            if section_id not in seen_nodes:
                sem_nodes.append({
                    "id": section_id,
                    "label": header_text,
                    "file_type": "document",
                    "source_file": rel_path,
                    "source_location": None,
                    "source_url": None,
                    "captured_at": None,
                    "author": None,
                    "contributor": None
                })
                seen_nodes.add(section_id)
                
                # Edge from document to header section
                sem_edges.append({
                    "source": doc_id,
                    "target": section_id,
                    "relation": "contains_section",
                    "confidence": "EXTRACTED",
                    "confidence_score": 1.0,
                    "source_file": rel_path,
                    "source_location": None,
                    "weight": 1.0
                })
                
                # Link related sections sequentially or structurally
                if prev_section_id and prev_section_id != doc_id:
                    sem_edges.append({
                        "source": prev_section_id,
                        "target": section_id,
                        "relation": "relates_to",
                        "confidence": "INFERRED",
                        "confidence_score": 0.8,
                        "source_file": rel_path,
                        "source_location": None,
                        "weight": 0.8
                    })
                prev_section_id = section_id

# Cross-link sprint-status items to epics.md stories
story_nodes = {n['id']: n for n in sem_nodes if 'epics_story_' in n['id']}
for n in list(sem_nodes):
    if n['id'].startswith('sprint_status_'):
        # Match story key prefix like 1_3 or 1-3
        m = re.search(r'sprint_status_(\d+)_(\d+)', n['id'])
        if m:
            epic_num, story_num = m.group(1), m.group(2)
            pattern = f"epics_story_{epic_num}_{story_num}_"
            for target_id in story_nodes:
                if target_id.startswith(pattern):
                    sem_edges.append({
                        "source": n['id'],
                        "target": target_id,
                        "relation": "governs_story_status",
                        "confidence": "EXTRACTED",
                        "confidence_score": 1.0,
                        "source_file": "_bmad-output/implementation-artifacts/sprint-status.yaml",
                        "source_location": None,
                        "weight": 1.0
                    })
        elif 'epic_' in n['id']:
            m = re.search(r'sprint_status_epic_(\d+)', n['id'])
            if m:
                epic_num = m.group(1)
                for target_node in sem_nodes:
                    if target_node['id'].startswith(f"epics_epic_{epic_num}_"):
                        sem_edges.append({
                            "source": n['id'],
                            "target": target_node['id'],
                            "relation": "governs_epic_status",
                            "confidence": "EXTRACTED",
                            "confidence_score": 1.0,
                            "source_file": "_bmad-output/implementation-artifacts/sprint-status.yaml",
                            "source_location": None,
                            "weight": 1.0
                        })

# Cross-link ARCHITECTURE-SPINE.md invariants to Epics and Agents
arch_spine_id = "doc_architecture_spine"
if arch_spine_id in seen_nodes:
    if "doc_epics" in seen_nodes:
        sem_edges.append({
            "source": arch_spine_id,
            "target": "doc_epics",
            "relation": "specifies_architecture_for",
            "confidence": "INFERRED",
            "confidence_score": 1.0,
            "source_file": "ARCHITECTURE-SPINE.md",
            "source_location": None,
            "weight": 1.0
        })
    if "doc_agents" in seen_nodes:
        sem_edges.append({
            "source": arch_spine_id,
            "target": "doc_agents",
            "relation": "governs_agent_policy",
            "confidence": "INFERRED",
            "confidence_score": 1.0,
            "source_file": "ARCHITECTURE-SPINE.md",
            "source_location": None,
            "weight": 1.0
        })
    if "doc_project_context" in seen_nodes:
        sem_edges.append({
            "source": arch_spine_id,
            "target": "doc_project_context",
            "relation": "authoritative_spine_for",
            "confidence": "INFERRED",
            "confidence_score": 1.0,
            "source_file": "ARCHITECTURE-SPINE.md",
            "source_location": None,
            "weight": 1.0
        })

# Link AD invariants to Epic 1 architectural bridge stories
ad_map = {
    "ad_1": ["epics_story_1_3_", "epics_story_1_4_", "epics_story_1_6_", "epics_story_1_7_"],
    "ad_2": ["epics_story_1_5_", "epics_story_1_7_"],
    "ad_3": ["epics_story_1_4_"],
    "ad_4": ["epics_story_1_3_", "epics_story_1_4_", "epics_story_1_10_"],
    "ad_5": ["epics_story_1_2_", "epics_story_1_3_"],
    "ad_6": ["epics_story_1_3_"],
    "ad_7": ["epics_story_1_1_", "epics_story_1_5_"],
    "ad_8": ["epics_story_1_4_", "epics_story_1_6_"]
}

for ad_key, story_prefixes in ad_map.items():
    ad_node_id = None
    for n in sem_nodes:
        if n['id'].startswith(f"architecture_spine_{ad_key}"):
            ad_node_id = n['id']
            break
    if ad_node_id:
        for prefix in story_prefixes:
            for sn_id in story_nodes:
                if sn_id.startswith(prefix):
                    sem_edges.append({
                        "source": ad_node_id,
                        "target": sn_id,
                        "relation": "governs_story",
                        "confidence": "EXTRACTED",
                        "confidence_score": 1.0,
                        "source_file": "ARCHITECTURE-SPINE.md",
                        "source_location": None,
                        "weight": 1.0
                    })

sem_data = {
    "nodes": sem_nodes,
    "edges": sem_edges,
    "hyperedges": [],
    "input_tokens": 0,
    "output_tokens": 0
}

Path('graphify-out/.graphify_semantic.json').write_text(json.dumps(sem_data, indent=2), encoding='utf-8')
print(f"Semantic extraction complete: {len(sem_nodes)} nodes, {len(sem_edges)} edges")

# Merge AST + Semantic
merged_nodes = list(ast_data['nodes'])
seen_ast = {n['id'] for n in ast_data['nodes']}
for n in sem_nodes:
    if n['id'] not in seen_ast:
        merged_nodes.append(n)
        seen_ast.add(n['id'])

merged_edges = ast_data['edges'] + sem_edges

merged = {
    "nodes": merged_nodes,
    "edges": merged_edges,
    "hyperedges": [],
    "input_tokens": 0,
    "output_tokens": 0
}

Path('graphify-out/.graphify_extract.json').write_text(json.dumps(merged, indent=2), encoding='utf-8')
print(f"Merged total: {len(merged_nodes)} nodes, {len(merged_edges)} edges")
