import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'

// Inline OptionRow component for testing: displays options and triggers onSelect
function OptionRow({ options = [], onSelect = () => {} }) {
  return (
    <div>
      {options.map((opt, i) => (
        <button key={i} data-testid={`opt-${i}`} onClick={() => onSelect(opt)}>
          {opt}
        </button>
      ))}
    </div>
  )
}

describe('OptionRow', () => {
  it('renders options and calls onSelect when clicked', async () => {
    const opts = ['A', 'B', 'C']
    const selected = []
    const handle = (o) => selected.push(o)

    render(<OptionRow options={opts} onSelect={handle} />)
    const user = userEvent.setup()

    await user.click(screen.getByTestId('opt-1'))
    expect(selected).toEqual(['B'])

    await user.click(screen.getByTestId('opt-0'))
    expect(selected).toEqual(['B', 'A'])
  })
})

