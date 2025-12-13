import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import random

# Ensure directories exist
os.makedirs("play_store", exist_ok=True)
os.makedirs("app/src/main/res/drawable-nodpi", exist_ok=True)

# Colors
GREEN_CALL = (76, 175, 80)
WHITE = (255, 255, 255)
DARK_BG = (30, 30, 30)
TEXT_COLOR = (0, 0, 0)
TEXT_COLOR_DARK = (255, 255, 255)

def create_icon():
    size = (512, 512)
    img = Image.new('RGBA', size, GREEN_CALL)
    draw = ImageDraw.Draw(img)
    
    # Draw phone handset
    # Simplified handset shape
    handset_coords = [
        (150, 350), (150, 250), (200, 200), (312, 200), (362, 250), (362, 350)
    ]
    # This is hard to draw manually with coords, let's draw a simple rounded rectangle and a "screen"
    # Actually, let's draw a classic handset shape using thick lines
    
    # Draw a thick arc
    draw.arc((128, 128, 384, 384), 180, 0, fill=WHITE, width=60)
    # Draw ends
    draw.ellipse((100, 220, 160, 280), fill=WHITE)
    draw.ellipse((352, 220, 412, 280), fill=WHITE)
    
    # Rotate 45 degrees
    img = img.rotate(45, resample=Image.BICUBIC, expand=False, fillcolor=GREEN_CALL)
    
    img.save("play_store/icon.png")
    print("Generated icon.png")

def create_feature_graphic():
    size = (1024, 500)
    img = Image.new('RGB', size, GREEN_CALL)
    draw = ImageDraw.Draw(img)
    
    try:
        font = ImageFont.truetype("arial.ttf", 80)
        font_small = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
        font_small = ImageFont.load_default()
        
    draw.text((50, 150), "Simple Phone", fill=WHITE, font=font)
    draw.text((50, 250), "Senior Friendly Dialer", fill=WHITE, font=font_small)
    
    # Draw some abstract shapes
    draw.ellipse((600, 50, 900, 350), outline=WHITE, width=10)
    draw.ellipse((700, 150, 1000, 450), outline=WHITE, width=10)
    
    img.save("play_store/feature_graphic.png")
    print("Generated feature_graphic.png")

def create_contact_image(name, filename, color):
    size = (400, 400)
    img = Image.new('RGB', size, color)
    draw = ImageDraw.Draw(img)
    
    # Draw a simple face
    draw.ellipse((100, 50, 300, 250), fill=(255, 220, 177)) # Face
    draw.ellipse((130, 120, 150, 140), fill=(0,0,0)) # Eye L
    draw.ellipse((250, 120, 270, 140), fill=(0,0,0)) # Eye R
    draw.arc((150, 180, 250, 220), 0, 180, fill=(0,0,0), width=5) # Smile
    
    # Add text
    try:
        font = ImageFont.truetype("arial.ttf", 40)
    except:
        font = ImageFont.load_default()
        
    # Draw name at bottom
    # draw.text((20, 350), name, fill=WHITE, font=font)
    
    img.save(f"app/src/main/res/drawable-nodpi/{filename}.png")
    print(f"Generated {filename}.png")

def create_screenshot_home():
    size = (1080, 1920)
    img = Image.new('RGB', size, WHITE)
    draw = ImageDraw.Draw(img)
    
    # Status bar
    draw.rectangle((0, 0, 1080, 60), fill=(200, 200, 200))
    
    # App Bar
    draw.rectangle((0, 60, 1080, 200), fill=GREEN_CALL)
    try:
        font_title = ImageFont.truetype("arial.ttf", 80)
        font_item = ImageFont.truetype("arial.ttf", 60)
    except:
        font_title = ImageFont.load_default()
        font_item = ImageFont.load_default()
        
    draw.text((50, 80), "Simple Phone", fill=WHITE, font=font_title)
    
    # Search Bar
    draw.rectangle((50, 220, 1030, 320), outline=(100,100,100), width=3)
    draw.text((80, 240), "Search contacts...", fill=(150,150,150), font=font_item)
    
    # Contacts
    y = 350
    contacts = [
        ("Grandson Tom", (100, 200, 255)),
        ("Martha (Bingo)", (255, 100, 100)),
        ("Dr. Smith", (100, 255, 100)),
        ("Emergency", (255, 0, 0))
    ]
    
    for name, color in contacts:
        # Item bg
        draw.rectangle((20, y, 1060, y+250), fill=(245, 245, 245))
        # Avatar
        draw.ellipse((50, y+25, 250, y+225), fill=color)
        # Name
        draw.text((280, y+90), name, fill=TEXT_COLOR, font=font_item)
        # Star
        draw.polygon([(950, y+100), (970, y+150), (930, y+150)], fill=(255, 215, 0))
        
        y += 270

    img.save("play_store/screenshot_1_home.png")
    print("Generated screenshot_1_home.png")

def create_screenshot_dialer():
    size = (1080, 1920)
    img = Image.new('RGB', size, WHITE)
    draw = ImageDraw.Draw(img)
    
    # Status bar
    draw.rectangle((0, 0, 1080, 60), fill=(200, 200, 200))
    
    # App Bar
    draw.rectangle((0, 60, 1080, 200), fill=GREEN_CALL)
    try:
        font_title = ImageFont.truetype("arial.ttf", 80)
        font_num = ImageFont.truetype("arial.ttf", 120)
        font_key = ImageFont.truetype("arial.ttf", 100)
    except:
        font_title = ImageFont.load_default()
        font_num = ImageFont.load_default()
        font_key = ImageFont.load_default()
        
    draw.text((50, 80), "Dialer", fill=WHITE, font=font_title)
    
    # Number display
    draw.text((100, 300), "0123 456 789", fill=TEXT_COLOR, font=font_num)
    
    # Keypad
    start_y = 600
    keys = [
        ['1', '2', '3'],
        ['4', '5', '6'],
        ['7', '8', '9'],
        ['*', '0', '#']
    ]
    
    for r, row in enumerate(keys):
        for c, key in enumerate(row):
            x = 150 + c * 300
            y = start_y + r * 250
            draw.ellipse((x, y, x+200, y+200), fill=(230, 230, 230))
            draw.text((x+70, y+50), key, fill=TEXT_COLOR, font=font_key)
            
    # Call button
    draw.ellipse((440, 1650, 640, 1850), fill=GREEN_CALL)
    
    img.save("play_store/screenshot_2_dialer.png")
    print("Generated screenshot_2_dialer.png")

if __name__ == "__main__":
    create_icon()
    create_feature_graphic()
    
    # Demo contacts
    create_contact_image("Grandson Tom", "demo_tom", (100, 200, 255))
    create_contact_image("Martha", "demo_martha", (255, 150, 150))
    create_contact_image("Dr. Smith", "demo_doctor", (150, 255, 150))
    create_contact_image("Sarah", "demo_sarah", (255, 200, 100))
    
    create_screenshot_home()
    create_screenshot_dialer()
