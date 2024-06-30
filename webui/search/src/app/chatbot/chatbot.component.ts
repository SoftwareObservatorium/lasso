// src/app/chatbot/chatbot.component.ts
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-chatbot',
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.css']
})
export class ChatbotComponent {
  
  @Input() context: any;

  messages: string[] = [];

  toggleChat() {
    this.context.toggleChat();
  }

  sendMessage(message: string) {
    // Add the message to the chat log
    this.messages.push(message);
  }
}