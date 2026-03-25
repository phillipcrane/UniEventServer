import { describe, it, expect } from 'vitest';
import { normalizeEvent } from '../../src/utils/eventNormalizerUtil';

describe('normalizeEvent', () => {
  it('should convert Facebook event to stored event format', () => {
    const fbEvent = {
      id: '123',
      name: 'Test Event',
      start_time: '2025-12-25T19:00:00+0100',
    };
    
    const result = normalizeEvent('test-page-id', fbEvent);
    
    expect(result.id).toBe('123');
    expect(result.pageId).toBe('test-page-id');
    expect(result.title).toBe('Test Event');
    expect(result.startTime).toBe('2025-12-25T19:00:00+0100');
  });

  it('should handle optional fields correctly', () => {
    const fbEvent = {
      id: '456',
      name: 'Minimal Event',
      start_time: '2025-12-25T19:00:00+0100',
      end_time: '2025-12-25T22:00:00+0100',
      description: 'Event description',
    };
    
    const result = normalizeEvent('page-123', fbEvent);
    
    expect(result.description).toBe('Event description');
    expect(result.endTime).toBe('2025-12-25T22:00:00+0100');
    expect(result.place).toBeNull();
    expect(result.coverImageUrl).toBeNull();
  });

  it('should include place when provided', () => {
    const fbEvent = {
      id: '789',
      name: 'Event with Place',
      start_time: '2025-12-25T19:00:00+0100',
      place: { name: 'DTU Building 101', id: 'place123' },
    };
    
    const result = normalizeEvent('page-456', fbEvent);
    
    expect(result.place).toEqual({ name: 'DTU Building 101', id: 'place123' });
  });

  it('should include cover image URL when provided', () => {
    const fbEvent = {
      id: '999',
      name: 'Event with Cover',
      start_time: '2025-12-25T19:00:00+0100',
      cover: { source: 'https://example.com/image.jpg' },
    };
    
    const result = normalizeEvent('page-789', fbEvent);
    
    expect(result.coverImageUrl).toBe('https://example.com/image.jpg');
  });

  it('should generate correct event URL', () => {
    const fbEvent = {
      id: '12345',
      name: 'URL Test Event',
      start_time: '2025-12-25T19:00:00+0100',
    };
    
    const result = normalizeEvent('page-999', fbEvent);
    
    expect(result.eventURL).toBe('https://facebook.com/events/12345');
  });
});