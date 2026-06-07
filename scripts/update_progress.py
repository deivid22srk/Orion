import os
import json
import re

def calculate_progress_details():
    config_path = "progress_config.json"
    if not os.path.exists(config_path):
        print("Arquivo progress_config.json nao encontrado!")
        return 0.0, []
        
    with open(config_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    total_weight = 0
    weighted_progress_sum = 0.0
    modules_progress = []
    
    for module in data.get("modules", []):
        name = module.get("name", "")
        weight = module.get("weight", 0)
        items = module.get("items", [])
        if not items:
            continue
            
        module_progress_sum = sum(item.get("progress", 0) for item in items)
        module_average = module_progress_sum / len(items)
        
        weighted_progress_sum += module_average * weight
        total_weight += weight
        
        modules_progress.append({
            "name": name,
            "weight": weight,
            "progress": round(module_average, 2)
        })
        
    if total_weight == 0:
        return 0.0, []
        
    overall_progress = weighted_progress_sum / total_weight
    return round(overall_progress, 2), modules_progress

def update_readme(overall_progress, modules_progress):
    readme_path = "README.md"
    if not os.path.exists(readme_path):
        print("README.md nao encontrado!")
        return
        
    with open(readme_path, "r", encoding="utf-8") as f:
        content = f.read()
        
    # Replace overall Shields.io badge
    new_badge = f"https://img.shields.io/badge/Progresso_Orion-{overall_progress}%25-emerald"
    updated_content = re.sub(
        r"https://img\.shields\.io/badge/Progresso_Orion-[\d\.]+%25-\w+",
        new_badge,
        content
    )
    
    # Update the overall progress bar inside the markdown
    filled_blocks = int(overall_progress // 10)
    empty_blocks = 10 - filled_blocks
    new_progress_bar = f"**Progresso Real Orion:** [{'█' * filled_blocks}{'░' * empty_blocks}] {overall_progress}%"
    updated_content = re.sub(
        r"\*\*Progresso Real Orion:\*\* \[.*\] [\d\.]+%",
        new_progress_bar,
        updated_content
    )
    
    # Generate large blocks progress lines
    block_lines = []
    for mod in modules_progress:
        mod_progress = mod["progress"]
        mod_filled = int(mod_progress // 10)
        mod_empty = 10 - mod_filled
        mod_bar = '█' * mod_filled + '░' * mod_empty
        line = f"* **{mod['name']} (Peso: {mod['weight']}%):** [{mod_bar}] {mod_progress}%"
        block_lines.append(line)
        
    blocks_markdown = "\n".join(block_lines)
    
    # Replace the block section in README.md
    pattern = r"<!-- BLOCKS_PROGRESS_START -->.*?<!-- BLOCKS_PROGRESS_END -->"
    replacement = f"<!-- BLOCKS_PROGRESS_START -->\n{blocks_markdown}\n<!-- BLOCKS_PROGRESS_END -->"
    updated_content = re.sub(pattern, replacement, updated_content, flags=re.DOTALL)
    
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(updated_content)
        
    print(f"README.md atualizado com sucesso! Progresso Ponderado Orion: {overall_progress}%")

if __name__ == "__main__":
    overall_progress, modules_progress = calculate_progress_details()
    update_readme(overall_progress, modules_progress)
