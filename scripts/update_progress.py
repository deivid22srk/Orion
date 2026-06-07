import os
import json
import re

def calculate_progress():
    config_path = "progress_config.json"
    if not os.path.exists(config_path):
        print("Arquivo progress_config.json nao encontrado!")
        return 0.0
        
    with open(config_path, "r", encoding="utf-8") as f:
        data = json.load(f)
        
    total_weight = 0
    weighted_progress_sum = 0.0
    
    for module in data.get("modules", []):
        weight = module.get("weight", 0)
        items = module.get("items", [])
        if not items:
            continue
            
        module_progress_sum = sum(item.get("progress", 0) for item in items)
        module_average = module_progress_sum / len(items)
        
        weighted_progress_sum += module_average * weight
        total_weight += weight
        
    if total_weight == 0:
        return 0.0
        
    overall_progress = weighted_progress_sum / total_weight
    return round(overall_progress, 2)

def update_readme(progress):
    readme_path = "README.md"
    if not os.path.exists(readme_path):
        print("README.md nao encontrado!")
        return
        
    with open(readme_path, "r", encoding="utf-8") as f:
        content = f.read()
        
    # Replace Shields.io badge with new weighted progress
    # Format: https://img.shields.io/badge/Progresso_Geral_Kotlin-XX.XX%25-blue (let's rename to Progresso_Orion for accuracy)
    new_badge = f"https://img.shields.io/badge/Progresso_Orion-{progress}%25-emerald"
    updated_content = re.sub(
        r"https://img\.shields\.io/badge/Progresso_Geral_Kotlin-[\d\.]+%25-blue",
        new_badge,
        content
    )
    # Support updating the new badge naming format as well
    updated_content = re.sub(
        r"https://img\.shields\.io/badge/Progresso_Orion-[\d\.]+%25-\w+",
        new_badge,
        updated_content
    )
    
    # Also update the progress bar inside the markdown
    # Format: **Progresso da Conversão Geral:** [████░░░░░░] XX.XX%
    filled_blocks = int(progress // 10)
    empty_blocks = 10 - filled_blocks
    new_progress_bar = f"**Progresso da Conversão Geral:** [{'█' * filled_blocks}{'░' * empty_blocks}] {progress}%"
    updated_content = re.sub(
        r"\*\*Progresso da Conversão Geral:\*\* \[.*\] [\d\.]+%",
        new_progress_bar,
        updated_content
    )
    
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(updated_content)
        
    print(f"README.md atualizado com sucesso! Progresso Real Orion: {progress}%")

if __name__ == "__main__":
    progress = calculate_progress()
    update_readme(progress)
