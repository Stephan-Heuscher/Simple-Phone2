const fs = require('fs');
const { PNG } = require('pngjs');
const path = require('path');

// Ensure directories
['play_store', 'app/src/main/res/drawable-nodpi'].forEach(dir => {
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
});

// Colors
const GREEN_CALL = [76, 175, 80];
const WHITE = [255, 255, 255];
const BLACK = [0, 0, 0];
const GRAY = [200, 200, 200];
const DARK_GRAY = [50, 50, 50];

// Simple 5x7 font map (very basic)
const FONT = {
    'A': [0x70, 0x88, 0x88, 0xF8, 0x88, 0x88, 0x88],
    'B': [0xF0, 0x88, 0x88, 0xF0, 0x88, 0x88, 0xF0],
    'C': [0x70, 0x88, 0x80, 0x80, 0x80, 0x88, 0x70],
    'D': [0xF0, 0x88, 0x88, 0x88, 0x88, 0x88, 0xF0],
    'E': [0xF8, 0x80, 0x80, 0xF0, 0x80, 0x80, 0xF8],
    'F': [0xF8, 0x80, 0x80, 0xF0, 0x80, 0x80, 0x80],
    'G': [0x70, 0x88, 0x80, 0xB8, 0x88, 0x88, 0x70],
    'H': [0x88, 0x88, 0x88, 0xF8, 0x88, 0x88, 0x88],
    'I': [0x70, 0x20, 0x20, 0x20, 0x20, 0x20, 0x70],
    'J': [0x08, 0x08, 0x08, 0x08, 0x08, 0x88, 0x70],
    'K': [0x88, 0x90, 0xA0, 0xC0, 0xA0, 0x90, 0x88],
    'L': [0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0xF8],
    'M': [0x88, 0xD8, 0xA8, 0x88, 0x88, 0x88, 0x88],
    'N': [0x88, 0xC8, 0xA8, 0x98, 0x88, 0x88, 0x88],
    'O': [0x70, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70],
    'P': [0xF0, 0x88, 0x88, 0xF0, 0x80, 0x80, 0x80],
    'Q': [0x70, 0x88, 0x88, 0x88, 0xA8, 0x90, 0x68],
    'R': [0xF0, 0x88, 0x88, 0xF0, 0xA0, 0x90, 0x88],
    'S': [0x70, 0x88, 0x80, 0x70, 0x08, 0x88, 0x70],
    'T': [0xF8, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20],
    'U': [0x88, 0x88, 0x88, 0x88, 0x88, 0x88, 0x70],
    'V': [0x88, 0x88, 0x88, 0x88, 0x88, 0x50, 0x20],
    'W': [0x88, 0x88, 0x88, 0x88, 0xA8, 0xD8, 0x88],
    'X': [0x88, 0x88, 0x50, 0x20, 0x50, 0x88, 0x88],
    'Y': [0x88, 0x88, 0x50, 0x20, 0x20, 0x20, 0x20],
    'Z': [0xF8, 0x08, 0x10, 0x20, 0x40, 0x80, 0xF8],
    '0': [0x70, 0x88, 0x98, 0xA8, 0xC8, 0x88, 0x70],
    '1': [0x20, 0x60, 0x20, 0x20, 0x20, 0x20, 0x70],
    '2': [0x70, 0x88, 0x08, 0x30, 0x40, 0x80, 0xF8],
    '3': [0xF8, 0x08, 0x10, 0x30, 0x08, 0x88, 0x70],
    '4': [0x10, 0x30, 0x50, 0x90, 0xF8, 0x10, 0x10],
    '5': [0xF8, 0x80, 0xF0, 0x08, 0x08, 0x88, 0x70],
    '6': [0x30, 0x40, 0x80, 0xF0, 0x88, 0x88, 0x70],
    '7': [0xF8, 0x08, 0x10, 0x20, 0x40, 0x40, 0x40],
    '8': [0x70, 0x88, 0x88, 0x70, 0x88, 0x88, 0x70],
    '9': [0x70, 0x88, 0x88, 0x78, 0x08, 0x10, 0x60],
    ' ': [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00],
    '.': [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20],
    '-': [0x00, 0x00, 0x00, 0x70, 0x00, 0x00, 0x00]
};

class Canvas {
    constructor(width, height, bgColor = WHITE) {
        this.width = width;
        this.height = height;
        this.png = new PNG({ width, height });
        this.fill(bgColor);
    }

    fill(color) {
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                this.setPixel(x, y, color);
            }
        }
    }

    setPixel(x, y, color, alpha = 255) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) return;
        const idx = (this.width * Math.floor(y) + Math.floor(x)) << 2;
        this.png.data[idx] = color[0];
        this.png.data[idx + 1] = color[1];
        this.png.data[idx + 2] = color[2];
        this.png.data[idx + 3] = alpha;
    }

    drawRect(x, y, w, h, color) {
        for (let i = 0; i < w; i++) {
            for (let j = 0; j < h; j++) {
                this.setPixel(x + i, y + j, color);
            }
        }
    }

    drawCircle(cx, cy, r, color) {
        for (let y = -r; y <= r; y++) {
            for (let x = -r; x <= r; x++) {
                if (x*x + y*y <= r*r) {
                    this.setPixel(cx + x, cy + y, color);
                }
            }
        }
    }
    
    drawArc(cx, cy, r, startAngle, endAngle, color, thickness = 1) {
        // Very naive implementation
        for (let t = startAngle; t <= endAngle; t += 0.01) {
            const x = cx + r * Math.cos(t);
            const y = cy + r * Math.sin(t);
            this.drawCircle(x, y, thickness, color);
        }
    }

    drawText(text, x, y, color, scale = 1) {
        text = text.toUpperCase();
        let cursorX = x;
        for (let i = 0; i < text.length; i++) {
            const char = text[i];
            const bitmap = FONT[char] || FONT[' '];
            
            for (let row = 0; row < 7; row++) {
                for (let col = 0; col < 5; col++) {
                    if ((bitmap[row] >> (7 - col)) & 1) {
                        this.drawRect(cursorX + col * scale, y + row * scale, scale, scale, color);
                    }
                }
            }
            cursorX += 6 * scale;
        }
    }

    save(filename) {
        const buffer = PNG.sync.write(this.png);
        fs.writeFileSync(filename, buffer);
        console.log(`Saved ${filename}`);
    }
}

// --- Generators ---

function createIcon() {
    const c = new Canvas(512, 512, GREEN_CALL);
    // Draw handset
    // Arc
    c.drawArc(256, 256, 120, Math.PI * 0.8, Math.PI * 2.2, WHITE, 20);
    // Ends
    c.drawCircle(170, 340, 30, WHITE);
    c.drawCircle(342, 340, 30, WHITE);
    c.save('play_store/icon.png');
}

function createFeatureGraphic() {
    const c = new Canvas(1024, 500, GREEN_CALL);
    c.drawText("SIMPLE PHONE", 100, 200, WHITE, 10);
    c.drawText("SENIOR FRIENDLY", 100, 300, WHITE, 5);
    c.save('play_store/feature_graphic.png');
}

function createContactImage(filename, bgColor) {
    const c = new Canvas(400, 400, bgColor);
    // Face
    c.drawCircle(200, 200, 120, [255, 220, 177]);
    // Eyes
    c.drawCircle(160, 180, 10, BLACK);
    c.drawCircle(240, 180, 10, BLACK);
    // Smile
    c.drawArc(200, 220, 50, 0.2, Math.PI - 0.2, BLACK, 3);
    
    c.save(`app/src/main/res/drawable-nodpi/${filename}.png`);
}

function createScreenshotHome() {
    const c = new Canvas(1080, 1920, WHITE);
    // Status bar
    c.drawRect(0, 0, 1080, 60, GRAY);
    // App bar
    c.drawRect(0, 60, 1080, 200, GREEN_CALL);
    c.drawText("SIMPLE PHONE", 50, 120, WHITE, 8);
    
    // Search
    c.drawRect(50, 280, 980, 100, [240, 240, 240]);
    c.drawText("SEARCH...", 80, 310, GRAY, 5);
    
    // Contacts
    let y = 420;
    const contacts = [
        ["GRANDSON TOM", [100, 200, 255]],
        ["MARTHA", [255, 150, 150]],
        ["DR SMITH", [150, 255, 150]]
    ];
    
    contacts.forEach(([name, color]) => {
        c.drawRect(20, y, 1040, 250, [250, 250, 250]);
        c.drawCircle(150, y + 125, 80, color);
        c.drawText(name, 280, y + 100, BLACK, 6);
        y += 270;
    });
    
    c.save('play_store/screenshot_1_home.png');
}

function createScreenshotDialer() {
    const c = new Canvas(1080, 1920, WHITE);
    // Status bar
    c.drawRect(0, 0, 1080, 60, GRAY);
    // App bar
    c.drawRect(0, 60, 1080, 200, GREEN_CALL);
    c.drawText("DIALER", 50, 120, WHITE, 8);
    
    // Number
    c.drawText("0123 456", 100, 400, BLACK, 12);
    
    // Keypad
    let startY = 700;
    const keys = [
        ['1', '2', '3'],
        ['4', '5', '6'],
        ['7', '8', '9'],
        ['*', '0', '#']
    ];
    
    keys.forEach((row, r) => {
        row.forEach((key, col) => {
            const x = 150 + col * 300;
            const y = startY + r * 250;
            c.drawCircle(x + 100, y + 100, 100, [230, 230, 230]);
            c.drawText(key, x + 70, y + 70, BLACK, 10);
        });
    });
    
    // Call button
    c.drawCircle(540, 1750, 100, GREEN_CALL);
    
    c.save('play_store/screenshot_2_dialer.png');
}

// Run
createIcon();
createFeatureGraphic();
createContactImage('demo_tom', [100, 200, 255]);
createContactImage('demo_martha', [255, 150, 150]);
createContactImage('demo_doctor', [150, 255, 150]);
createContactImage('demo_sarah', [255, 200, 100]);
createScreenshotHome();
createScreenshotDialer();
